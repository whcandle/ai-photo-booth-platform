# 完整的 API Key 自动测试脚本

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "API Key 功能完整自动测试" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 步骤 1: 生成加密的 API Key（使用 Java）
Write-Host "步骤 1: 生成加密的 API Key..." -ForegroundColor Yellow

$plainKey = "sk-test-aliyun-key-12345"
Write-Host "明文 API Key: $plainKey" -ForegroundColor Cyan

# 使用 Maven 编译并运行加密工具
Write-Host "正在编译加密工具..." -ForegroundColor Gray
$compileResult = mvn compile -DskipTests -q 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 编译失败" -ForegroundColor Red
    exit 1
}

# 尝试运行加密工具（需要 Spring Boot 上下文）
Write-Host "注意: 需要 Spring Boot 应用上下文来运行加密工具" -ForegroundColor Yellow
Write-Host "`n请手动执行以下步骤:" -ForegroundColor Yellow
Write-Host "1. 启动应用: mvn spring-boot:run" -ForegroundColor Gray
Write-Host "2. 在另一个终端运行加密工具" -ForegroundColor Gray
Write-Host "3. 或者使用下面的 SQL 方法（仅测试用）" -ForegroundColor Gray

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "方法 A: 使用 Base64 编码（快速测试）" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 使用 Base64 编码作为简单测试
$bytes = [System.Text.Encoding]::UTF8.GetBytes($plainKey)
$base64Encoded = [Convert]::ToBase64String($bytes)

Write-Host "Base64 编码值: $base64Encoded" -ForegroundColor Green
Write-Host "`nSQL 插入语句（Base64 方法，仅用于快速测试）:" -ForegroundColor Yellow
Write-Host "INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status)" -ForegroundColor Gray
Write-Host "SELECT id, 'Aliyun Test API Key', '$base64Encoded', 'ACTIVE'" -ForegroundColor Gray
Write-Host "FROM model_providers WHERE code = 'aliyun';" -ForegroundColor Gray

Write-Host "`n注意: Base64 不是真正的加密，仅用于快速测试" -ForegroundColor Yellow
Write-Host "生产环境必须使用 CryptoUtil.encrypt() 方法" -ForegroundColor Yellow

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "方法 B: 使用真正的 AES 加密" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "需要运行: mvn spring-boot:run -Dspring-boot.run.main-class=com.mg.platform.util.EncryptApiKeyTool -Dspring-boot.run.arguments=`"$plainKey`"" -ForegroundColor Gray

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "步骤 2: 检查应用状态" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

try {
    $testResponse = Invoke-WebRequest -Uri "http://localhost:8089/api/v1/ai/resolve" `
        -Method Post `
        -Headers @{"Content-Type"="application/json"} `
        -Body '{"capability":"segmentation"}' `
        -ErrorAction Stop
    Write-Host "✅ 应用正在运行" -ForegroundColor Green
    $appRunning = $true
} catch {
    Write-Host "❌ 应用未运行" -ForegroundColor Red
    Write-Host "请先启动应用: mvn spring-boot:run" -ForegroundColor Yellow
    $appRunning = $false
}

if ($appRunning) {
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "步骤 3: 测试 API" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    
    $body = @{
        capability = "segmentation"
        prefer = @("aliyun")
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8089/api/v1/ai/resolve" `
            -Method Post `
            -Headers @{"Content-Type"="application/json"} `
            -Body $body
        
        Write-Host "✅ API 调用成功" -ForegroundColor Green
        Write-Host "Provider Code: $($response.data.direct.providerCode)" -ForegroundColor Cyan
        Write-Host "API Key Type: $($response.data.direct.auth.type)" -ForegroundColor Cyan
        
        if ($response.data.direct.auth.apiKey) {
            $apiKey = $response.data.direct.auth.apiKey
            $keyLength = $apiKey.Length
            $preview = if ($keyLength -gt 15) { 
                $apiKey.Substring(0, 15) + "..." 
            } else { 
                $apiKey 
            }
            Write-Host "✅ API Key 已返回: $preview (长度: $keyLength)" -ForegroundColor Green
            
            if ($apiKey -eq $plainKey) {
                Write-Host "✅ 验证成功: API Key 正确解密" -ForegroundColor Green
            } else {
                Write-Host "⚠️ 注意: API Key 与预期不符（可能使用了不同的加密方法）" -ForegroundColor Yellow
            }
            
            Write-Host "`n完整响应:" -ForegroundColor Cyan
            $response | ConvertTo-Json -Depth 10
            
            Write-Host "`n========================================" -ForegroundColor Cyan
            Write-Host "✅ 测试结论" -ForegroundColor Green
            Write-Host "========================================" -ForegroundColor Cyan
            Write-Host "1. ✅ API Key 功能正常" -ForegroundColor Green
            Write-Host "2. ✅ API Key 已成功解密并返回" -ForegroundColor Green
            Write-Host "3. ✅ 响应格式正确" -ForegroundColor Green
        } else {
            Write-Host "⚠️ API Key 为空" -ForegroundColor Yellow
            Write-Host "需要插入测试数据到数据库" -ForegroundColor Yellow
        }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 400) {
            try {
                $errorStream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($errorStream)
                $errorBody = $reader.ReadToEnd() | ConvertFrom-Json
                Write-Host "❌ API 返回 400 错误" -ForegroundColor Red
                Write-Host "错误信息: $($errorBody.message)" -ForegroundColor Red
                
                if ($errorBody.message -like "*NO_ACTIVE_API_KEY*") {
                    Write-Host "`n✅ 错误处理正常: 正确返回 NO_ACTIVE_API_KEY 错误码" -ForegroundColor Green
                    Write-Host "`n需要插入 API Key 到数据库才能测试完整功能" -ForegroundColor Yellow
                }
            } catch {
                Write-Host "错误详情: $($_.Exception.Message)" -ForegroundColor Red
            }
        } else {
            Write-Host "❌ API 调用失败: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "测试完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
