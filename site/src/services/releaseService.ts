import { ReleaseInfo, ReleaseAsset } from '../types';

export const REPO_OWNER = 'weianweigan';
export const REPO_NAME = 'KrPlay';
export const GITHUB_REPO_URL = `https://github.com/${REPO_OWNER}/${REPO_NAME}`;
export const GITHUB_RELEASES_URL = `${GITHUB_REPO_URL}/releases`;

const FALLBACK_TAG = 'v0.4.2';

const FALLBACK_RELEASE: ReleaseInfo = {
  tag: FALLBACK_TAG,
  name: `KrPlay ${FALLBACK_TAG}`,
  publishedAt: '2026-10-19',
  htmlUrl: `${GITHUB_RELEASES_URL}/tag/${FALLBACK_TAG}`,
  body: '优化车机大屏触控延迟，完善双向音频麦克风调度，更新借网 VPN 服务稳定性。',
  assets: {
    receiverApk: {
      name: 'KrPlay-debug.apk',
      size: 16800000,
      downloadUrl: `${GITHUB_RELEASES_URL}/download/${FALLBACK_TAG}/KrPlay-debug.apk`,
    },
    clientApk: {
      name: 'KrPlay-Client-debug.apk',
      size: 12400000,
      downloadUrl: `${GITHUB_RELEASES_URL}/download/${FALLBACK_TAG}/KrPlay-Client-debug.apk`,
    },
    sourceZip: {
      name: `KrPlay-source-${FALLBACK_TAG}.zip`,
      size: 4500000,
      downloadUrl: `${GITHUB_RELEASES_URL}/download/${FALLBACK_TAG}/KrPlay-source-${FALLBACK_TAG}.zip`,
    },
    licensesZip: {
      name: 'KrPlay-licenses.zip',
      size: 210000,
      downloadUrl: `${GITHUB_RELEASES_URL}/download/${FALLBACK_TAG}/KrPlay-licenses.zip`,
    },
    artifactsJson: {
      name: 'artifacts.json',
      size: 2048,
      downloadUrl: `${GITHUB_RELEASES_URL}/download/${FALLBACK_TAG}/artifacts.json`,
    },
    all: [],
  },
  isLive: false,
};

export async function fetchLatestRelease(): Promise<ReleaseInfo> {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 4000);

    const response = await fetch(`https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases/latest`, {
      signal: controller.signal,
      headers: {
        Accept: 'application/vnd.github.v3+json',
      },
    });
    clearTimeout(timeoutId);

    if (!response.ok) {
      console.warn(`GitHub API returned status ${response.status}, falling back to static release info.`);
      return FALLBACK_RELEASE;
    }

    const data = await response.json();
    const rawAssets: any[] = data.assets || [];

    const allAssets: ReleaseAsset[] = rawAssets.map(a => ({
      name: a.name,
      size: a.size,
      downloadUrl: a.browser_download_url,
      contentType: a.content_type,
    }));

    const receiverApk = allAssets.find(a => a.name.toLowerCase().includes('client') === false && a.name.endsWith('.apk'));
    const clientApk = allAssets.find(a => a.name.toLowerCase().includes('client') && a.name.endsWith('.apk'));
    const sourceZip = allAssets.find(a => a.name.toLowerCase().includes('source') && a.name.endsWith('.zip'));
    const licensesZip = allAssets.find(a => a.name.toLowerCase().includes('license') && a.name.endsWith('.zip'));
    const artifactsJson = allAssets.find(a => a.name === 'artifacts.json');

    return {
      tag: data.tag_name || FALLBACK_TAG,
      name: data.name || `KrPlay ${data.tag_name}`,
      publishedAt: data.published_at ? data.published_at.split('T')[0] : '2026-10-19',
      htmlUrl: data.html_url || `${GITHUB_RELEASES_URL}/tag/${data.tag_name}`,
      body: data.body || '',
      assets: {
        receiverApk: receiverApk || {
          name: 'KrPlay-debug.apk',
          size: 16800000,
          downloadUrl: `${GITHUB_RELEASES_URL}/download/${data.tag_name}/KrPlay-debug.apk`,
        },
        clientApk: clientApk || {
          name: 'KrPlay-Client-debug.apk',
          size: 12400000,
          downloadUrl: `${GITHUB_RELEASES_URL}/download/${data.tag_name}/KrPlay-Client-debug.apk`,
        },
        sourceZip: sourceZip || {
          name: `KrPlay-source-${data.tag_name}.zip`,
          size: 4500000,
          downloadUrl: `${GITHUB_RELEASES_URL}/download/${data.tag_name}/KrPlay-source-${data.tag_name}.zip`,
        },
        licensesZip: licensesZip || {
          name: 'KrPlay-licenses.zip',
          size: 210000,
          downloadUrl: `${GITHUB_RELEASES_URL}/download/${data.tag_name}/KrPlay-licenses.zip`,
        },
        artifactsJson,
        all: allAssets,
      },
      isLive: true,
    };
  } catch (err) {
    console.warn('Failed to fetch from GitHub API, using fallback data:', err);
    return FALLBACK_RELEASE;
  }
}

export function formatBytes(bytes: number, decimals = 1): string {
  if (!+bytes) return '0 B';
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
}
