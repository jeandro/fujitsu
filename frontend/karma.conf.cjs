const fs = require("fs");
// ChromeHeadless também pode executar o Chromium distribuído com o Edge.
if (!process.env.CHROME_BIN && process.platform === "win32") {
  for (const base of [
    process.env.PROGRAMFILES,
    process.env["PROGRAMFILES(X86)"],
    process.env.LOCALAPPDATA,
  ].filter(Boolean)) {
    for (const suffix of [
      "/Google/Chrome/Application/chrome.exe",
      "/Microsoft/Edge/Application/msedge.exe",
    ]) {
      if (fs.existsSync(base + suffix)) {
        process.env.CHROME_BIN = base + suffix;
        break;
      }
    }
    if (process.env.CHROME_BIN) break;
  }
}
module.exports = (config) =>
  config.set({
    frameworks: ["jasmine", "@angular-devkit/build-angular"],
    plugins: [
      require("karma-jasmine"),
      require("karma-chrome-launcher"),
      require("karma-coverage"),
      require("@angular-devkit/build-angular/plugins/karma"),
    ],
    reporters: ["progress"],
    browsers: process.env.KARMA_MANUAL ? [] : ["ChromeHeadless"],
    singleRun: true,
    coverageReporter: {
      dir: require("path").join(__dirname, "coverage"),
      reporters: [{ type: "html" }, { type: "text-summary" }],
    },
  });
