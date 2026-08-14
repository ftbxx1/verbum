# Verbum test runner — runs the acceptance demo and the full unit test suite.
$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot

Write-Host "== Building engine ==" -ForegroundColor Cyan
mvn -q -pl verbum-engine -am package | Out-Null

Write-Host "`n== Acceptance demo (offline mock world) ==" -ForegroundColor Cyan
java -cp "verbum-engine\target\classes" dev.verbum.cli.VerbumCli demo

Write-Host "`n== Unit tests (lexer, parser, interpreter) ==" -ForegroundColor Cyan
mvn -pl verbum-engine test

Write-Host "`n== All sample scripts validated ==" -ForegroundColor Cyan
$files = @("verbum-engine\src\main\resources\scripts\game.mcscript",
           "verbum-engine\src\main\resources\scripts\acceptance.vb")
$files += Get-ChildItem "verbum-engine\src\main\resources\scripts\examples\*.vb" |
    ForEach-Object { $_.FullName }
foreach ($f in $files) {
    $out = java -cp "verbum-engine\target\classes" dev.verbum.cli.VerbumCli check $f 2>&1
    if ($LASTEXITCODE -ne 0) { Write-Host "FAIL: $f" -ForegroundColor Red; continue }
    Write-Host ("OK   " + (Split-Path $f -Leaf))
}

Write-Host "`nDone." -ForegroundColor Green
