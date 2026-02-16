# API Key 功能完整测试脚本

$baseUrl = "http://localhost:8089/api/v1/ai/resolve"
$headers = @{"Content-Type" = "application/json"}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "API Key 功能完整测试" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 测试 1: 有 API Key 的情况（aliyun）
Write-Host "=== 测试 1: 有 API Key（aliyun） ===" -ForegroundColor Green
$body1 = @{
    capability = "segmentation"
    prefer = @("aliyun")
} | ConvertTo-Json

try {
    $response1 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body1
    Write-Host "✅ 成功" -ForegroundColor Green
    Write-Host "Provider Code: $($response1.data.direct.providerCode)" -ForegroundColor Yellow
    Write-Host "API Key Type: $($response1.data.direct.auth.type)" -ForegroundColor Yellow
    if ($response1.data.direct.auth.apiKey) {
        $keyLength = $response1.data.direct.auth.apiKey.Length
        $preview = if ($keyLength -gt 10) { 
            $response1.data.direct.auth.apiKey.Substring(0, 10) + "..." 
        } else { 
            $response1.data.direct.auth.apiKey 
        }
        Write-Host "API Key (预览): $preview" -ForegroundColor Yellow
        Write-Host "API Key 长度: $keyLength 字符" -ForegroundColor Yellow
    } else {
        Write-Host "⚠️ 警告: API Key 为空" -ForegroundColor Red
    }
    Write-Host "`n完整响应:" -ForegroundColor Cyan
    $response1 | ConvertTo-Json -Depth 10
} catch {
    Write-Host "❌ 失败: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        try {
            $errorJson = $_.ErrorDetails.Message | ConvertFrom-Json
            Write-Host "错误响应: " -NoNewline
            $errorJson | ConvertTo-Json -Depth 10
        } catch {
            Write-Host "错误详情: $($_.ErrorDetails.Message)" -ForegroundColor Red
        }
    }
}

Write-Host "`n" -NoNewline

# 测试 2: 测试 volc（如果有 API Key）
Write-Host "=== 测试 2: 有 API Key（volc） ===" -ForegroundColor Green
$body2 = @{
    capability = "segmentation"
    prefer = @("volc")
} | ConvertTo-Json

try {
    $response2 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body2
    Write-Host "✅ 成功" -ForegroundColor Green
    Write-Host "Provider Code: $($response2.data.direct.providerCode)" -ForegroundColor Yellow
    if ($response2.data.direct.auth.apiKey) {
        $keyLength = $response2.data.direct.auth.apiKey.Length
        $preview = if ($keyLength -gt 10) { 
            $response2.data.direct.auth.apiKey.Substring(0, 10) + "..." 
        } else { 
            $response2.data.direct.auth.apiKey 
        }
        Write-Host "API Key (预览): $preview" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ 失败: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        try {
            $errorJson = $_.ErrorDetails.Message | ConvertFrom-Json
            Write-Host "错误响应: " -NoNewline
            $errorJson | ConvertTo-Json -Depth 10
        } catch {
            Write-Host "错误详情: $($_.ErrorDetails.Message)" -ForegroundColor Red
        }
    }
}

Write-Host "`n" -NoNewline

# 测试 3: 无 API Key 的情况（需要先禁用 API Key）
Write-Host "=== 测试 3: 无 API Key 的情况 ===" -ForegroundColor Yellow
Write-Host "提示: 需要先在数据库中禁用 API Key 才能测试此场景" -ForegroundColor Yellow
Write-Host "SQL: UPDATE provider_api_keys SET status = 'INACTIVE' WHERE provider_id = (SELECT id FROM model_providers WHERE code = 'aliyun');" -ForegroundColor Gray

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "测试完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "验证要点:" -ForegroundColor Cyan
Write-Host "1. ✅ API Key 是否正确解密并返回" -ForegroundColor Green
Write-Host "2. ✅ 检查应用日志，确认明文 API Key 不会出现在日志中" -ForegroundColor Green
Write-Host "3. ✅ 验证 providerCode 和 endpoint 是否正确" -ForegroundColor Green
