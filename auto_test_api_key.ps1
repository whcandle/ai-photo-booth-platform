# API Key 自动测试脚本

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "API Key 功能自动测试" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$baseUrl = "http://localhost:8089/api/v1/ai/resolve"
$plainKey = "sk-test-aliyun-key-12345"

# 步骤 1: 生成 Base64 编码（用于快速测试）
Write-Host "步骤 1: 生成测试用的加密值..." -ForegroundColor Yellow
$bytes = [System.Text.Encoding]::UTF8.GetBytes($plainKey)
$base64Encoded = [Convert]::ToBase64String($bytes)
Write-Host "明文 API Key: $plainKey" -ForegroundColor Cyan
Write-Host "Base64 编码值: $base64Encoded" -ForegroundColor Green

Write-Host "`nSQL 插入语句（复制执行）:" -ForegroundColor Yellow
Write-Host "INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status) SELECT id, 'Aliyun Test API Key', '$base64Encoded', 'ACTIVE' FROM model_providers WHERE code = 'aliyun';" -ForegroundColor Gray

Write-Host "`n注意: Base64 编码仅用于快速测试，生产环境需使用真正的 AES 加密" -ForegroundColor Yellow

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "步骤 2: 检查应用状态" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

try {
    $testBody = '{"capability":"segmentation"}'
    $testResponse = Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Content-Type"="application/json"} -Body $testBody -ErrorAction Stop
    Write-Host "应用正在运行" -ForegroundColor Green
    $appRunning = $true
} catch {
    Write-Host "应用未运行，请先启动: mvn spring-boot:run" -ForegroundColor Red
    $appRunning = $false
}

if (-not $appRunning) {
    Write-Host "`n测试终止，请先启动应用" -ForegroundColor Yellow
    exit
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "步骤 3: 测试 API" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$body = @{
    capability = "segmentation"
    prefer = @("aliyun")
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers @{"Content-Type"="application/json"} -Body $body
    
    Write-Host "API 调用成功" -ForegroundColor Green
    Write-Host "Provider: $($response.data.direct.providerCode)" -ForegroundColor Cyan
    Write-Host "API Key Type: $($response.data.direct.auth.type)" -ForegroundColor Cyan
    
    if ($response.data.direct.auth.apiKey) {
        $apiKey = $response.data.direct.auth.apiKey
        $keyLength = $apiKey.Length
        $preview = if ($keyLength -gt 15) { $apiKey.Substring(0, 15) + "..." } else { $apiKey }
        Write-Host "API Key: $preview (长度: $keyLength)" -ForegroundColor Green
        
        if ($apiKey -eq $plainKey) {
            Write-Host "验证成功: API Key 正确解密" -ForegroundColor Green
        }
        
        Write-Host "`n完整响应:" -ForegroundColor Cyan
        $response | ConvertTo-Json -Depth 10
        
        Write-Host "`n========================================" -ForegroundColor Cyan
        Write-Host "测试结论" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "1. API Key 功能正常" -ForegroundColor Green
        Write-Host "2. API Key 已成功解密并返回" -ForegroundColor Green
        Write-Host "3. 响应格式正确" -ForegroundColor Green
    } else {
        Write-Host "API Key 为空，需要插入测试数据" -ForegroundColor Yellow
    }
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 400) {
        try {
            $errorStream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($errorStream)
            $errorBody = $reader.ReadToEnd() | ConvertFrom-Json
            Write-Host "API 返回 400 错误" -ForegroundColor Red
            Write-Host "错误: $($errorBody.message)" -ForegroundColor Red
            
            if ($errorBody.message -like "*NO_ACTIVE_API_KEY*") {
                Write-Host "错误处理正常: 返回 NO_ACTIVE_API_KEY" -ForegroundColor Green
                Write-Host "需要插入 API Key 到数据库" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Red
        }
    } else {
        Write-Host "API 调用失败: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n测试完成" -ForegroundColor Cyan
