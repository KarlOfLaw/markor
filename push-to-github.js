/**
 * 一键推送 Markor 源码到 GitHub 并触发 Actions 构建
 *
 * 使用方式（在项目目录下打开终端执行）：
 *   set PAT=你的PAT令牌 && node push-to-github.js
 *
 * 前提：已在浏览器创建 PAT（见下方说明）
 */
const https = require('https');
const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const PAT = process.env.PAT;
if (!PAT) {
  console.error('='.repeat(60));
  console.error('请先创建 GitHub PAT 令牌！');
  console.error('='.repeat(60));
  console.error('\n创建步骤：');
  console.error('  1. 打开 https://github.com/settings/tokens');
  console.error('  2. 点击 "Generate new token" → "Generate new token (classic)"');
  console.error('  3. 勾选以下两个 Scope：');
  console.error('     ☑ repo（全部子项）— 推送源码');
  console.error('     ☑ workflow — 推送 .github/workflows 文件（关键！）');
  console.error('  4. 生成后复制令牌');
  console.error('  5. 在终端执行：');
  console.error('     export PAT=你的令牌');
  console.error(`     ${process.argv[0]} ${path.basename(__filename)}`);
  process.exit(1);
}

const OWNER = 'KarlOfLaw';
const REPO = 'markor';
const REPO_URL = `https://${OWNER}:${PAT}@github.com/${OWNER}/${REPO}.git`;
const API_BASE = `https://api.github.com/repos/${OWNER}/${REPO}`;
const PROJECT_DIR = process.cwd();

async function apiCall(method, urlPath, body = null) {
  return new Promise((resolve, reject) => {
    const url = new URL(urlPath.startsWith('http') ? urlPath : `${API_BASE}${urlPath}`);
    const options = {
      hostname: url.hostname,
      path: url.pathname + url.search,
      method,
      headers: {
        'Authorization': `token ${PAT}`,
        'Accept': 'application/vnd.github.v3+json',
        'User-Agent': 'WorkBuddy-Push',
        'Content-Type': 'application/json',
      },
      rejectUnauthorized: false,
    };
    const req = https.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => {
        try { resolve({ status: res.statusCode, data: JSON.parse(data) }); }
        catch { resolve({ status: res.statusCode, data }); }
      });
    });
    req.on('error', reject);
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

function run(cmd) {
  console.log(`  $ ${cmd}`);
  return execSync(cmd, { cwd: PROJECT_DIR, encoding: 'utf8', stdio: 'pipe' });
}

async function main() {
  console.log('\n=== 第1步：检查 GitHub 仓库 ===\n');

  // 1. 检查仓库是否已存在
  const check = await apiCall('GET', '');
  let repoExists = check.status === 200;

  if (!repoExists) {
    console.log('  仓库不存在，创建中...');
    const create = await apiCall('POST', 'https://api.github.com/user/repos', {
      name: REPO,
      description: 'Markor - A markdown editor for Android',
      private: false,
    });
    if (create.status === 201) {
      console.log(`  ✅ 仓库创建成功: https://github.com/${OWNER}/${REPO}`);
    } else {
      console.error(`  ❌ 创建失败: ${create.status} ${JSON.stringify(create.data)}`);
      process.exit(1);
    }
  } else {
    console.log(`  ✅ 仓库已存在: ${check.data.html_url}`);
  }

  // 2. 检查本地 git 状态
  console.log('\n=== 第2步：检查本地 Git 状态 ===\n');
  try {
    const status = run('git status --porcelain').trim();
    if (status) {
      console.log('  有未提交的修改，正在提交...');
      run('git add -A');
      run('git commit -m "Update: local modifications for CI build"');
      console.log('  ✅ 已提交');
    } else {
      console.log('  ✅ 工作区干净');
    }
  } catch (e) {
    console.log('  工作区干净或已是最新');
  }

  // 3. 设置 remote 并推送
  console.log('\n=== 第3步：推送到 GitHub ===\n');

  // 检查当前分支
  let branch;
  try {
    branch = run('git branch --show-current').trim();
  } catch {
    branch = 'master';
  }
  console.log(`  当前分支: ${branch}`);

  // 设置/更新 remote
  try {
    run('git remote remove origin');
  } catch {}
  run(`git remote add origin ${REPO_URL}`);
  console.log('  ✅ remote 已设置');

  // 推送
  console.log(`  正在推送 ${branch} 分支...`);
  try {
    const pushResult = run(`git push -u origin ${branch}`);
    console.log('  ✅ 推送成功！');
  } catch (e) {
    console.error(`  ❌ 推送失败: ${e.message.substring(0, 200)}`);
    process.exit(1);
  }

  // 4. 确认 Actions 构建
  console.log('\n=== 第4步：确认 Actions 已触发 ===\n');
  const sleep = (ms) => new Promise(r => setTimeout(r, ms));
  await sleep(3000);

  const runs = await apiCall('GET', '/actions/runs?per_page=5');
  if (runs.status === 200 && runs.data.workflow_runs?.length > 0) {
    const latest = runs.data.workflow_runs[0];
    console.log(`  ✅ 构建已自动触发!`);
    console.log(`  状态: ${latest.status} — ${latest.conclusion || '运行中'}`);
    console.log(`  链接: ${latest.html_url}`);
  } else {
    console.log('  尚未检测到构建触发，请手动检查:');
    console.log(`  https://github.com/${OWNER}/${REPO}/actions`);
  }

  console.log('\n' + '='.repeat(60));
  console.log('🎉 完成！后续构建请访问:');
  console.log(`📦 仓库: https://github.com/${OWNER}/${REPO}`);
  console.log(`⚙️  Actions: https://github.com/${OWNER}/${REPO}/actions`);
  console.log('='.repeat(60));
}

main().catch(e => console.error('脚本异常:', e.message));
