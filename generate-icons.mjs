#!/usr/bin/env node

import { execSync } from 'child_process';
import { copyFileSync, rmSync, existsSync } from 'fs';
import { dirname, join } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const SOURCE_DIR = './app_icons/tauri_icons';
const TARGET_ICONS = {
  macos: {
    source: join(SOURCE_DIR, 'icon.icns'),
    target: 'composeApp/src/jvmMain/resources/icons/macos/app.icns'
  },
  windows: {
    source: join(SOURCE_DIR, 'icon.ico'),
    target: 'composeApp/src/jvmMain/resources/icons/windows/app.ico'
  },
  linux: {
    source: join(SOURCE_DIR, 'icon.png'),
    target: 'composeApp/src/jvmMain/resources/icons/linux/app.png'
  },
  compose: {
    source: join(SOURCE_DIR, 'icon.png'),
    target: 'composeApp/src/jvmMain/composeResources/drawable/app_icon.png'
  }
};

function runCommand(command, description) {
  try {
    console.log(`\n${description}...`);
    execSync(command, { stdio: 'inherit', cwd: __dirname });
    console.log(`✓ ${description} 完成`);
  } catch (error) {
    console.error(`✗ ${description} 失败:`, error.message);
    process.exit(1);
  }
}

function copyFile(source, target, description) {
  try {
    if (!existsSync(source)) {
      console.error(`✗ 源文件不存在: ${source}`);
      process.exit(1);
    }
    copyFileSync(source, target);
    console.log(`✓ ${description}: ${source} → ${target}`);
  } catch (error) {
    console.error(`✗ 复制文件失败:`, error.message);
    process.exit(1);
  }
}

function deleteDirectory(dir, description) {
  try {
    if (existsSync(dir)) {
      rmSync(dir, { recursive: true, force: true });
      console.log(`✓ ${description}: ${dir}`);
    } else {
      console.log(`ℹ 目录不存在，跳过删除: ${dir}`);
    }
  } catch (error) {
    console.error(`✗ 删除目录失败:`, error.message);
    process.exit(1);
  }
}

console.log('========================================');
console.log('  开始生成并替换应用图标');
console.log('========================================');

// 步骤 1: 生成图标，需要提前安装图标生成工具 cargo +nightly install generate-app-icons
runCommand('generate-app-icons gen ./app-icon.png', '生成应用图标到 ./app_icons');

// 步骤 2: 复制图标文件到对应位置
console.log('\n复制图标文件到目标位置...');
copyFile(TARGET_ICONS.macos.source, TARGET_ICONS.macos.target, '复制 macOS 图标');
copyFile(TARGET_ICONS.windows.source, TARGET_ICONS.windows.target, '复制 Windows 图标');
copyFile(TARGET_ICONS.linux.source, TARGET_ICONS.linux.target, '复制 Linux 图标');
copyFile(TARGET_ICONS.compose.source, TARGET_ICONS.compose.target, '复制 Compose 资源图标');

// 步骤 3: 删除临时目录
deleteDirectory('./app_icons', '删除临时图标目录');

console.log('\n========================================');
console.log('  ✓ 图标生成和替换完成！');
console.log('========================================');
