# KrPlay 官网 (Site)

KrPlay 官方展示与下载站点，介绍双端协作架构（闲置安卓端 KrPlay 与车机端 KrPlay-Client），并提供最新版本 APK、完整源码与开源许可证的下载与直链获取。

## 技术栈

- **框架**：Vite 6 + React 19 + TypeScript
- **样式**：Tailwind CSS（深色座舱风格为主，支持深浅色切换、毛玻璃效果与发光动效）
- **图标**：Lucide React
- **国际化**：内置中英双语切换（默认简体中文）
- **数据**：动态获取 GitHub Releases API，兼具本地兜底与一键复制直链能力

## 本地开发

```bash
cd site
npm install
npm run dev
```

浏览器打开控制台输出的本地地址（默认 `http://localhost:5173`）即可预览。

## 生产构建

```bash
npm run build
```

构建产物输出至 `site/dist/` 目录。本地预览构建产物：

```bash
npm run preview
```

## Cloudflare Pages 部署说明

在 Cloudflare Dashboard 中连接本仓库，或直接上传 `site/dist`：

1. **Framework Preset**: None 或 Vite
2. **Build command**: `cd site && npm install && npm run build` (若项目根目录设为 `site`，则直接填 `npm run build`)
3. **Build output directory**: `dist` (或根目录构建时的 `site/dist`)
4. **Node.js Version**: 18+ (推荐 20 或 22)
