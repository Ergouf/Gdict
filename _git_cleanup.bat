@echo off
set PATH=C:\Program Files\Git\cmd;%PATH%
cd /d d:\workspace\Gdict

echo === Step 1: filter-branch ===
git filter-branch --force --index-filter "git rm -r --cached --ignore-unmatch desktop/app/resources/windows-x64/jcef-bundle/" --prune-empty -- --all

echo === Step 2: clean up refs ===
rd /s /q .git\refs\original 2>nul

echo === Step 3: expire reflog ===
git reflog expire --expire=now --all

echo === Step 4: gc prune ===
git gc --prune=now --aggressive

echo === Step 5: count objects ===
git count-objects -vH

echo === Done ===
