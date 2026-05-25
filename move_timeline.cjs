const fs = require('fs');
const filePath = '/home/iredox/Desktop/personal-apps/quran-app/src/pages/Planner.jsx';
let content = fs.readFileSync(filePath, 'utf8');

// Find the timeline block
const timelineStartIdx = content.indexOf('<div className="flex flex-col">\n                                    <div className="mb-3 flex items-baseline justify-between px-1">\n                                        <h2 className="font-ui text-[1.1rem] font-semibold text-[var(--text-primary)]">Timeline</h2>');
if (timelineStartIdx === -1) {
    console.error("Could not find Timeline start");
    process.exit(1);
}

// Find the end of the timeline block (it ends with a div that closes the flex-col)
// The end is before: {weeklySummary && weeklySummary.length > 0 && (
const weeklySummaryIdx = content.indexOf('{weeklySummary && weeklySummary.length > 0 && (');
if (weeklySummaryIdx === -1) {
    console.error("Could not find Weekly Summary");
    process.exit(1);
}

// Find the closing div of the timeline block
const timelineEndSnippet = '</span>\n                                        ))}\n                                    </div>\n                                </div>\n';
const timelineEndIdx = content.indexOf(timelineEndSnippet, timelineStartIdx);
if (timelineEndIdx === -1 || timelineEndIdx > weeklySummaryIdx) {
    console.error("Could not find Timeline end");
    process.exit(1);
}

const timelineBlock = content.substring(timelineStartIdx, timelineEndIdx + timelineEndSnippet.length);
console.log("Found timeline block of length", timelineBlock.length);

// Remove the timeline block
content = content.substring(0, timelineStartIdx) + content.substring(timelineEndIdx + timelineEndSnippet.length);

// Add mb-8 to the timeline block for spacing
const modifiedTimelineBlock = timelineBlock.replace('<div className="flex flex-col">', '<div className="flex flex-col mb-8">');

// Find the daily ritual block
const dailyRitualTarget = '<div className="flex-1 w-full">\n                                    <div className="mb-4 flex items-center justify-between">\n                                        <h2 className="font-ui text-[1.35rem] font-semibold tracking-[0.01em] text-[var(--text-primary)]">Daily Ritual</h2>';

const targetIdx = content.indexOf(dailyRitualTarget);
if (targetIdx === -1) {
    console.error("Could not find Daily Ritual target");
    process.exit(1);
}

// Insert timeline before Daily Ritual
const insertIdx = targetIdx + '<div className="flex-1 w-full">\n'.length;
content = content.substring(0, insertIdx) + '                                    ' + modifiedTimelineBlock.trim() + '\n\n' + content.substring(insertIdx);

fs.writeFileSync(filePath, content);
console.log("Moved timeline successfully");
