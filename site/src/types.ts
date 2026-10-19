export type Language = 'zh' | 'en';

export interface ReleaseAsset {
  name: string;
  size: number;
  downloadUrl: string;
  contentType?: string;
}

export interface ReleaseInfo {
  tag: string;
  name: string;
  publishedAt: string;
  htmlUrl: string;
  body: string;
  assets: {
    receiverApk?: ReleaseAsset;
    clientApk?: ReleaseAsset;
    sourceZip?: ReleaseAsset;
    licensesZip?: ReleaseAsset;
    artifactsJson?: ReleaseAsset;
    all: ReleaseAsset[];
  };
  isLive: boolean;
}
