const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const projectRoot = path.resolve(__dirname, '..');
const outDir = path.resolve(projectRoot, 'src/main/resources/static/js');
const outFile = path.resolve(outDir, 'winter.js');

if (!fs.existsSync(outDir)) {
  fs.mkdirSync(outDir, { recursive: true });
}

// Browser shim for log4js required by Winter.LoggerProvider
const shimPath = path.resolve('/tmp', 'log4js-browser-shim.js');
fs.writeFileSync(shimPath, `
module.exports = {
  getLogger: function(name) {
    return {
      info: function() { console.info.apply(console, ['[' + name + ']'].concat(Array.prototype.slice.call(arguments))); },
      warn: function() { console.warn.apply(console, ['[' + name + ']'].concat(Array.prototype.slice.call(arguments))); },
      error: function() { console.error.apply(console, ['[' + name + ']'].concat(Array.prototype.slice.call(arguments))); },
      debug: function() { console.debug.apply(console, ['[' + name + ']'].concat(Array.prototype.slice.call(arguments))); }
    };
  }
};
`);

const winterEntry = path.resolve(projectRoot, 'node_modules/winter/lib/index.js');
const cmd = `npx --yes esbuild "${winterEntry}" --bundle --format=iife --global-name=Winter --alias:log4js="${shimPath}" --outfile="${outFile}"`;

console.log('Building winter.js bundle from node_modules/winter...');
execSync(cmd, { stdio: 'inherit', cwd: projectRoot });
console.log('Successfully generated:', outFile);
