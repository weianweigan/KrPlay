// npm install --no-save --package-lock=false sharp; node scripts/generate_app_icons.cjs
// Export-time only. Keep the original Figma SVGs intact as the source of truth.
const fs = require('node:fs/promises');
const path = require('node:path');
const sharp = require('sharp');
const root = path.resolve(__dirname, '..');
async function png(source, destination, size) {
  const target = path.join(root, destination);
  await fs.mkdir(path.dirname(target), { recursive: true });
  await sharp(source, { density: 384 }).resize(size, size).png().toFile(target);
}
async function main() {
  for (const [name, res] of [
    ['krplay', 'krplay/shared/src/main/res'],
    ['krplay-client', 'krplay-client/src/main/res'],
  ]) {
    const source = await fs.readFile(path.join(root, `docs/assets/icons/${name}.svg`));
    await png(source, `${res}/drawable-nodpi/ic_launcher_artwork.png`, 1024);
    for (const [density, size] of Object.entries({mdpi:48, hdpi:72, xhdpi:96, xxhdpi:144, xxxhdpi:192})) {
      for (const suffix of ['', '_round']) {
        const target = path.join(root, res, `mipmap-${density}`, `ic_launcher${suffix}.webp`);
        await fs.mkdir(path.dirname(target), {recursive:true});
        await sharp(source, {density:384}).resize(size,size).webp({lossless:true}).toFile(target);
      }
    }
    await png(source, `docs/assets/icons/${name}.png`, 512);
    const color = name === 'krplay' ? '#00B94F' : '#0B1B3A';
    const adaptive = `<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@color/ic_launcher_background" />\n    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n</adaptive-icon>\n`;
    await fs.mkdir(path.join(root,res,'mipmap-anydpi-v26'), {recursive:true});
    for (const suffix of ['', '_round']) await fs.writeFile(path.join(root,res,'mipmap-anydpi-v26',`ic_launcher${suffix}.xml`),adaptive);
    await fs.mkdir(path.join(root,res,'drawable'), {recursive:true});
    // Visible Figma artwork fits in the adaptive icon's central 66dp safe circle.
    await fs.writeFile(path.join(root,res,'drawable/ic_launcher_foreground.xml'), `<?xml version="1.0" encoding="utf-8"?>\n<inset xmlns:android="http://schemas.android.com/apk/res/android" android:drawable="@drawable/ic_launcher_artwork" android:inset="18dp" />\n`);
    await fs.mkdir(path.join(root,res,'values'), {recursive:true});
    await fs.writeFile(path.join(root,res,'values/ic_launcher_colors.xml'),`<?xml version="1.0" encoding="utf-8"?>\n<resources><color name="ic_launcher_background">${color}</color></resources>\n`);
  }
  await png(await fs.readFile(path.join(root,'docs/assets/icons/krplay.svg')), 'krplay/shared/src/main/ic_launcher-playstore.png',512);
  await fs.copyFile(path.join(root,'docs/assets/icons/krplay.svg'),path.join(root,'krplay/docs/assets/app-icon.svg'));
  await png(await fs.readFile(path.join(root,'docs/assets/icons/zeekr.svg')), 'krplay/common/src/main/res/raw/ic_car_home.png',256);
  console.log('Generated both Android launcher sets, documentation icons and Zeekr return icon.');
}
main().catch(error => { console.error(error); process.exitCode=1; });
