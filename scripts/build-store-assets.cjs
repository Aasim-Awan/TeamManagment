// Rebuild Play assets from real Compose captures. No customer data or downloaded artwork.
const fs = require('fs');
const path = require('path');
const sharp = require(process.env.TEAMFUND_SHARP_MODULE || 'sharp');
const root = path.resolve(__dirname, '..');
const output = path.join(root, 'store-assets');
const raw = path.join(output, 'raw');
const charcoal = '#20231E', lime = '#D5F58A', paper = '#F3F4F1';
const escape = value => value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
const svg = (width, height, body) => Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">${body}</svg>`);
const text = (x, y, value, size, weight = 600, color = charcoal) => `<text x="${x}" y="${y}" font-family="Arial, sans-serif" font-size="${size}" font-weight="${weight}" fill="${color}">${escape(value)}</text>`;
const screens = [
  ['01-home', ['Your team.', 'One clear view.'], 'Your fund balance and this month\'s spending.', 'Team dashboard showing the available fund, money in, spending this month and quick actions.'],
  ['02-players', ['Every player.', 'Every balance.'], 'Keep your roster and dues organized.', 'Player roster with search, roles, contributions and outstanding expense shares.'],
  ['03-matches', ['Plan the match.', 'Track the result.'], 'Fixtures, venues and scores in one place.', 'Match list showing upcoming fixtures, venues and a recorded match result.'],
  ['04-reports', ['Make the numbers', 'make sense.'], 'Review finances. Export CSV reports.', 'Financial report showing fund balance, contributions, expense totals and category breakdown.'],
  ['05-add-expense', ['Shared costs.', 'Clear records.'], 'Log expenses and choose who shares them.', 'Expense form with title, amount, date, category and payer fields.'],
  ['06-settings', ['Your team.', 'Your preferences.'], 'Teams, seasons, dues and CSV in one place.', 'Settings screen with team switcher, currency, seasons, monthly dues, theme and data options.'],
  ['07-all-expenses', ['Every expense.', 'One timeline.'], 'Filter by month, type or player.', 'All expenses timeline grouped by month and day with total spent, fund and player payments, and filters.'],
  ['08-settlements', ['Track dues.', 'Record settlements.'], 'See unpaid shares and player balances.', 'Settlement screen showing player dues and controls to record payments.']
];

async function main() {
  fs.mkdirSync(output, { recursive: true });
  fs.mkdirSync(path.join(output, 'screenshots'), { recursive: true });
  const foreground = fs.readFileSync(path.join(root, 'app/src/main/res/drawable/ic_app_logo_foreground.xml'), 'utf8');
  const paths = [...foreground.matchAll(/<path\s+android:fillColor="([^"]+)"\s+android:pathData="([^"]+)"\s*\/>/g)]
    .map(([, fill, d]) => `<path fill="${fill}" d="${d}"/>`).join('');
  if (!paths) throw Error('Launcher vector paths not found');
  const icon = svg(512, 512, `<rect width="512" height="512" fill="${charcoal}"/><g transform="translate(-26 -26) scale(5.22)">${paths}</g>`);
  fs.writeFileSync(path.join(output, 'icon-source.svg'), icon);
  await sharp(icon).png().toFile(path.join(output, 'play-icon-512.png'));
  const manifest = { generatedFrom: 'Real app Compose screenshots; isolated fictional Northside Cricket records', icon: 'play-icon-512.png', featureGraphic: 'feature-graphic-1024x500.png', screenshots: [] };
  for (const [name, title, caption, alt] of screens) {
    const source = path.join(raw, `${name}.png`);
    if (!fs.existsSync(source)) throw Error(`Missing real app capture: ${source}. Run StoreScreenshotTest first.`);
    const header = svg(1080, 1920, `<rect width="1080" height="1920" fill="${paper}"/><rect x="72" y="66" width="160" height="42" rx="21" fill="${lime}"/>${text(96, 95, 'TEAMFUND', 23, 700)}${text(72, 192, title[0], 64)}${text(72, 267, title[1], 64)}${text(72, 325, caption, 28, 400, '#5D6258')}<rect x="100" y="382" width="880" height="1480" rx="32" fill="#FFFFFF"/>`);
    const image = await sharp(source).flatten({ background: paper }).resize({ width: 872, height: 1472, fit: 'inside' }).png().toBuffer();
    const dimensions = await sharp(image).metadata();
    const target = path.join(output, 'screenshots', `${name}.png`);
    await sharp(header).composite([{ input: image, left: Math.round((1080 - dimensions.width) / 2), top: 386 }])
      .flatten({ background: paper }).removeAlpha().png().toFile(target);
    manifest.screenshots.push({ file: `screenshots/${name}.png`, width: 1080, height: 1920, alt });
  }
  const dashboard = await sharp(path.join(raw, '01-home.png')).flatten({ background: paper })
    .resize({ width: 250, height: 442, fit: 'inside' }).png().toBuffer();
  const feature = svg(1024, 500, `<rect width="1024" height="500" fill="${lime}"/><circle cx="1000" cy="60" r="300" fill="#C5EC7C"/><circle cx="940" cy="460" r="220" fill="none" stroke="#B2D96A" stroke-width="2"/>${text(68, 115, 'TeamFund Manager', 26, 600)}${text(68, 213, 'Your team.', 60)}${text(68, 289, 'One clear view.', 60)}${text(70, 358, 'Players, fixtures and shared expenses.', 23, 400)}${text(70, 392, 'Organized on your device.', 23, 400)}`);
  await sharp(feature).composite([{ input: dashboard, left: 704, top: 28 }]).flatten({ background: lime }).removeAlpha().png()
    .toFile(path.join(output, 'feature-graphic-1024x500.png'));
  const thumbs = [];
  for (let i = 0; i < screens.length; i++) {
    thumbs.push({ input: await sharp(path.join(output, 'screenshots', `${screens[i][0]}.png`)).resize(360, 640).toBuffer(), left: (i % 4) * 384 + 24, top: Math.floor(i / 4) * 664 + 24 });
  }
  await sharp({ create: { width: 1560, height: 1352, channels: 3, background: '#E2E6DD' } })
    .composite(thumbs).jpeg({ quality: 90 }).toFile(path.join(output, 'preview.jpg'));
  manifest.featureAlt = 'TeamFund Manager with a team dashboard and the message Your team. One clear view.';
  fs.writeFileSync(path.join(output, 'asset-manifest.json'), JSON.stringify(manifest, null, 2));
  for (const file of ['play-icon-512.png', 'feature-graphic-1024x500.png', ...manifest.screenshots.map(s => s.file)]) {
    const full = path.join(output, file), meta = await sharp(full).metadata();
    if (file.startsWith('screenshots/') && (meta.width !== 1080 || meta.height !== 1920 || meta.hasAlpha)) throw Error(`Invalid screenshot ${file}`);
    if (file.startsWith('feature-') && (meta.width !== 1024 || meta.height !== 500 || meta.hasAlpha)) throw Error('Invalid feature graphic');
    if (file.startsWith('play-icon') && (meta.width !== 512 || meta.height !== 512 || fs.statSync(full).size > 1024 * 1024)) throw Error('Invalid store icon');
    console.log(`${file}: ${meta.width}x${meta.height}, ${meta.channels} channels, ${fs.statSync(full).size} bytes`);
  }
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
