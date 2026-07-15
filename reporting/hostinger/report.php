<?php
declare(strict_types=1);

const REPORT_PACKAGE = 'ai.openclaw.app.hud';
const REPORT_RECIPIENT = 'earlvanze@gmail.com';
const MAX_BODY_BYTES = 16384;
const MAX_REPORTS_PER_DAY = 250;
const RETENTION_SECONDS = 7776000;

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');

function respond(int $status, array $payload): never
{
    http_response_code($status);
    echo json_encode($payload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

function clean_text(mixed $value, int $maxLength): string
{
    if (!is_string($value)) {
        return '';
    }
    $value = preg_replace('/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/u', '', trim($value)) ?? '';
    return mb_substr($value, 0, $maxLength, 'UTF-8');
}

function consume_daily_capacity(string $storageDir): bool
{
    $counterPath = $storageDir . '/count-' . gmdate('Y-m-d') . '.txt';
    $handle = fopen($counterPath, 'c+');
    if ($handle === false || !flock($handle, LOCK_EX)) {
        if (is_resource($handle)) {
            fclose($handle);
        }
        return false;
    }

    $raw = stream_get_contents($handle);
    $count = is_string($raw) ? (int) trim($raw) : 0;
    if ($count >= MAX_REPORTS_PER_DAY) {
        flock($handle, LOCK_UN);
        fclose($handle);
        return false;
    }

    rewind($handle);
    ftruncate($handle, 0);
    fwrite($handle, (string) ($count + 1));
    fflush($handle);
    flock($handle, LOCK_UN);
    fclose($handle);
    return true;
}

function prune_old_files(string $storageDir): void
{
    $cutoff = time() - RETENTION_SECONDS;
    foreach (glob($storageDir . '/reports-*.jsonl') ?: [] as $path) {
        $modified = filemtime($path);
        if ($modified !== false && $modified < $cutoff) {
            @unlink($path);
        }
    }
    foreach (glob($storageDir . '/count-*.txt') ?: [] as $path) {
        $modified = filemtime($path);
        if ($modified !== false && $modified < 172800) {
            @unlink($path);
        }
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    respond(200, [
        'ok' => true,
        'service' => 'openclaw-hud-content-reports',
        'retentionDays' => 90,
    ]);
}

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    header('Allow: GET, POST, OPTIONS');
    respond(204, []);
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Allow: GET, POST, OPTIONS');
    respond(405, ['ok' => false, 'error' => 'method_not_allowed']);
}

$contentLength = (int) ($_SERVER['CONTENT_LENGTH'] ?? 0);
if ($contentLength <= 0 || $contentLength > MAX_BODY_BYTES) {
    respond(413, ['ok' => false, 'error' => 'invalid_body_size']);
}

$raw = file_get_contents('php://input', false, null, 0, MAX_BODY_BYTES + 1);
if (!is_string($raw) || strlen($raw) > MAX_BODY_BYTES) {
    respond(413, ['ok' => false, 'error' => 'invalid_body_size']);
}

try {
    $input = json_decode($raw, true, 16, JSON_THROW_ON_ERROR);
} catch (JsonException) {
    respond(400, ['ok' => false, 'error' => 'invalid_json']);
}

if (!is_array($input) || ($input['packageName'] ?? null) !== REPORT_PACKAGE) {
    respond(400, ['ok' => false, 'error' => 'invalid_package']);
}

$categories = ['hate_or_abuse', 'sexual_content', 'violence', 'self_harm', 'illegal_or_dangerous', 'other_offensive'];
$category = clean_text($input['category'] ?? null, 40);
$appVersion = clean_text($input['appVersion'] ?? null, 40);
$assistantExcerpt = clean_text($input['assistantExcerpt'] ?? null, 4000);
$userComment = clean_text($input['userComment'] ?? null, 500);
$messageHash = strtolower(clean_text($input['messageHash'] ?? null, 64));

if (!in_array($category, $categories, true)) {
    respond(400, ['ok' => false, 'error' => 'invalid_category']);
}
if (!preg_match('/^[0-9A-Za-z._-]{1,40}$/', $appVersion)) {
    respond(400, ['ok' => false, 'error' => 'invalid_app_version']);
}
if ($assistantExcerpt === '') {
    respond(400, ['ok' => false, 'error' => 'missing_excerpt']);
}
if (!preg_match('/^[a-f0-9]{64}$/', $messageHash)) {
    respond(400, ['ok' => false, 'error' => 'invalid_message_hash']);
}

$storageDir = dirname(__DIR__) . '/.openclaw-hud-reports';
if (!is_dir($storageDir) && !mkdir($storageDir, 0700, true) && !is_dir($storageDir)) {
    respond(503, ['ok' => false, 'error' => 'storage_unavailable']);
}
if (!consume_daily_capacity($storageDir)) {
    respond(429, ['ok' => false, 'error' => 'daily_capacity_reached']);
}

$receipt = bin2hex(random_bytes(16));
$record = [
    'receipt' => $receipt,
    'receivedAt' => gmdate('c'),
    'packageName' => REPORT_PACKAGE,
    'appVersion' => $appVersion,
    'category' => $category,
    'assistantExcerpt' => $assistantExcerpt,
    'userComment' => $userComment === '' ? null : $userComment,
    'messageHash' => $messageHash,
];
$line = json_encode($record, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE) . "\n";
$reportPath = $storageDir . '/reports-' . gmdate('Y-m') . '.jsonl';
if (file_put_contents($reportPath, $line, FILE_APPEND | LOCK_EX) === false) {
    respond(503, ['ok' => false, 'error' => 'storage_unavailable']);
}
@chmod($reportPath, 0600);

$subject = '[OpenClaw HUD report] ' . $category . ' ' . substr($receipt, 0, 8);
$mailBody = "Receipt: {$receipt}\nVersion: {$appVersion}\nCategory: {$category}\nMessage hash: {$messageHash}\n\nAssistant excerpt:\n{$assistantExcerpt}";
if ($userComment !== '') {
    $mailBody .= "\n\nUser comment:\n{$userComment}";
}
@mail(REPORT_RECIPIENT, $subject, $mailBody, "From: OpenClaw HUD <noreply@aops.studio>\r\nContent-Type: text/plain; charset=UTF-8");

prune_old_files($storageDir);
respond(201, ['ok' => true, 'receipt' => $receipt]);
