# 校园论坛冒烟测试（当前系统：com.cainiao.forum，Session 认证，58080 端口）
# 用法：后端启动后执行 powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:58080'
# 所有请求统一携带防伪请求头（后端 CsrfFilter 校验写请求必须携带）。
$h = @{ 'X-Requested-With' = 'XMLHttpRequest' }

function Assert-True($cond, $label) {
  if (-not $cond) { throw "$label failed" }
  Write-Host "PASS $label" -ForegroundColor Green
}

# 1. 版块列表（匿名）
$boards = Invoke-RestMethod "$base/board/topList" -Headers $h
Assert-True ($boards.code -eq 0) 'board topList'
Assert-True ($boards.data.Count -ge 1) "boards count = $($boards.data.Count)"

# 2. 帖子分页列表（匿名）
$articles = Invoke-RestMethod "$base/article/getAllByBoardId?page=1&size=10" -Headers $h
Assert-True ($articles.code -eq 0) 'article page list'
Assert-True ($articles.data.total -ge 1) "article total = $($articles.data.total)"

# 3. 匿名查看帖子详情
$articleId = $articles.data.records[0].id
$detail = Invoke-RestMethod "$base/article/details?id=$articleId" -Headers $h
Assert-True ($detail.code -eq 0) "anonymous article detail (id=$articleId)"

# 4. CSRF 防护：写请求缺少 X-Requested-With 头应被 403 拒绝
$csrfBlocked = $false
try {
  Invoke-WebRequest "$base/article/thumbsUp" -Method Post -Body 'id=1' `
    -ContentType 'application/x-www-form-urlencoded' -UseBasicParsing -ErrorAction Stop | Out-Null
} catch {
  $csrfBlocked = ($_.Exception.Response -and $_.Exception.Response.StatusCode.value__ -eq 403)
}
Assert-True $csrfBlocked 'csrf blocks write without X-Requested-With'

# 5. 学生登录 + 当前用户信息
$stuLogin = Invoke-RestMethod "$base/user/login" -Method Post -Headers $h `
  -Body 'username=stu2026001&password=123456' `
  -ContentType 'application/x-www-form-urlencoded' -SessionVariable stu
Assert-True ($stuLogin.code -eq 0) 'student login'
$stuInfo = Invoke-RestMethod "$base/user/info" -Headers $h -WebSession $stu
Assert-True ($stuInfo.code -eq 0 -and $stuInfo.data.username -eq 'stu2026001') 'student session info'

# 6. 学生越权访问后台统计应被拒绝（403 Forbidden 或 code 1003）
$stuAdminCode = -1
try {
  $stuAdmin = Invoke-RestMethod "$base/admin/summary" -Headers $h -WebSession $stu
  $stuAdminCode = $stuAdmin.code
} catch {
  # 后端 LoginInterceptor 可能直接返回 403 HTTP 状态码
  $stuAdminCode = [int]$_.Exception.Response.StatusCode.value__
}
Assert-True ($stuAdminCode -eq 403 -or $stuAdminCode -eq 1003) "student forbidden admin (code=$stuAdminCode)"

# 7. 管理员登录 + 后台概览统计
$admLogin = Invoke-RestMethod "$base/user/login" -Method Post -Headers $h `
  -Body 'username=admin&password=admin123' `
  -ContentType 'application/x-www-form-urlencoded' -SessionVariable adm
Assert-True ($admLogin.code -eq 0) 'admin login'
$admSummary = Invoke-RestMethod "$base/admin/summary" -Headers $h -WebSession $adm
Assert-True ($admSummary.code -eq 0) 'admin summary'
Write-Host "INFO admin summary users=$($admSummary.data.users) boards=$($admSummary.data.boards) articles=$($admSummary.data.articles)"

Write-Host 'SMOKE TEST ALL PASS' -ForegroundColor Cyan
