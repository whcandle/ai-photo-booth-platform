# Resolve API 测试脚本 (PowerShell)

$baseUrl = "http://localhost:8089/api/v1/ai/resolve"
$headers = @{"Content-Type" = "application/json"}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Resolve API 测试脚本" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 测试 1: 基础测试
Write-Host "=== 测试 1: 基础测试（无 prefer） ===" -ForegroundColor Green
$body1 = @{
    capability = "segmentation"
} | ConvertTo-Json
try {
    $response1 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body1
    $response1 | ConvertTo-Json -Depth 10
} catch {
    Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Red
    }
}

Write-Host "`n" -NoNewline

# 测试 2: 带 prefer 参数
Write-Host "=== 测试 2: 带 prefer 参数 ===" -ForegroundColor Green
$body2 = @{
    capability = "segmentation"
    prefer = @("volc", "aliyun")
} | ConvertTo-Json
try {
    $response2 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body2
    $response2 | ConvertTo-Json -Depth 10
} catch {
    Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Red
    }
}

Write-Host "`n" -NoNewline

# 测试 3: 带 constraints 和 hintParams
Write-Host "=== 测试 3: 带 constraints 和 hintParams ===" -ForegroundColor Green
$body3 = @{
    capability = "segmentation"
    prefer = @("aliyun")
    constraints = @{
        timeoutMs = 15000
    }
    hintParams = @{
        quality = "ultra"
        format = "png"
    }
} | ConvertTo-Json -Depth 10
try {
    $response3 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body3
    $response3 | ConvertTo-Json -Depth 10
} catch {
    Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Red
    }
}

Write-Host "`n" -NoNewline

# 测试 4: 测试 background_generation
Write-Host "=== 测试 4: background_generation capability ===" -ForegroundColor Green
$body4 = @{
    capability = "background_generation"
    prefer = @("volc")
} | ConvertTo-Json
try {
    $response4 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body4
    $response4 | ConvertTo-Json -Depth 10
} catch {
    Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Red
    }
}

Write-Host "`n" -NoNewline

# 测试 5: 错误场景 - capability 不存在
Write-Host "=== 测试 5: 错误场景（capability 不存在） ===" -ForegroundColor Yellow
$body5 = @{
    capability = "non_existent_capability"
} | ConvertTo-Json
try {
    $response5 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body5
    $response5 | ConvertTo-Json -Depth 10
} catch {
    Write-Host "预期错误: $($_.Exception.Message)" -ForegroundColor Yellow
    if ($_.ErrorDetails) {
        $errorJson = $_.ErrorDetails.Message | ConvertFrom-Json
        Write-Host "错误响应: " -NoNewline
        $errorJson | ConvertTo-Json -Depth 10
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "测试完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
