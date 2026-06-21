import { Client, Databases, Permission, Role, ID } from 'node-appwrite';
import fs from 'fs';

// Parse .env manually
const envVars = {};
const envFile = fs.readFileSync('.env', 'utf8');
envFile.split('\n').forEach(line => {
    const match = line.match(/^\s*([\w.-]+)\s*=\s*(.*)?\s*$/);
    if (match) {
        let key = match[1];
        let value = (match[2] || '').trim();
        if (value.startsWith('"') && value.endsWith('"')) value = value.slice(1, -1);
        envVars[key] = value;
    }
});

const client = new Client()
    .setEndpoint(envVars['VITE_APPWRITE_ENDPOINT'])
    .setProject(envVars['VITE_APPWRITE_PROJECT_ID'])
    .setKey(envVars['APPWRITE_API_KEY']);

const databases = new Databases(client);
const databaseId = envVars['VITE_APPWRITE_DATABASE_ID'];

const GROUPS_ID = 'sauka_groups';
const COMMENTS_ID = 'sauka_comments';

async function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

async function updateCollections() {
    console.log(`Updating Sauka collections in DB: ${databaseId}...`);

    try {
        // 1. Add intention attribute to sauka_groups
        console.log(`Adding 'intention' attribute to ${GROUPS_ID}...`);
        try {
            await databases.createStringAttribute(databaseId, GROUPS_ID, 'intention', 500, false);
            console.log("Added 'intention' attribute. Waiting for processing...");
            await sleep(2000);
        } catch (e) {
            console.log("Error adding intention attribute (might already exist):", e.message);
        }

        // 2. Create sauka_comments Collection
        console.log(`\nCreating collection: ${COMMENTS_ID}...`);
        try {
            await databases.createCollection(databaseId, COMMENTS_ID, 'Sauka Comments', [
                Permission.read(Role.users()),
                Permission.create(Role.users()),
                Permission.update(Role.users()),
                Permission.delete(Role.users())
            ]);
            console.log('Collection created.');

            // Add Attributes
            await databases.createStringAttribute(databaseId, COMMENTS_ID, 'groupId', 50, true);
            await databases.createStringAttribute(databaseId, COMMENTS_ID, 'userId', 50, true);
            await databases.createStringAttribute(databaseId, COMMENTS_ID, 'userName', 100, true);
            await databases.createStringAttribute(databaseId, COMMENTS_ID, 'text', 1000, true);
            console.log('Added attributes to sauka_comments. Waiting for processing...');
            await sleep(2000);

            // Add Index
            try {
                await databases.createIndex(databaseId, COMMENTS_ID, 'idx_groupId', 'key', ['groupId']);
            } catch (e) {
                if (e.code !== 409) console.log('Index creation error:', e.message);
            }
        } catch (e) {
            console.log("Error creating comments collection (might already exist):", e.message);
        }

        console.log('\n✅ Successfully updated Appwrite collections for Sauka features!');

    } catch (error) {
        console.error('❌ Update failed:', error.message);
    }
}

updateCollections();
