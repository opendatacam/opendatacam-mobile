## OpenDataCam Mobile

### Documented diff between main repo (for the node.js / next.js part)

Diff between main opendatacam node.js app and node.js app for mobile

https://github.com/opendatacam/opendatacam/compare/development...mobile

Will need to build adapters / if(Android) in the code to have the same codebase

### Build app

```bash

# Copy web assets to native projects
npx cap copy

# Go to main opendatacam project to build the node.js app
cd ../opendatacam
# checkout mobile branch

# build front-end code and export app as static
npm i
npm run build
npm run export

# prune node_module from dev dependencies
npm prune --production

# pack as a zip (ignore useless folder) and put in assets folder of the mobile project

# delete previous zip
rm ../opendatacam-mobile/android/app/src/main/assets/nodejs-project.zip

# zip new one (only include necessary folders)
zip -0 -r ../opendatacam-mobile/android/app/src/main/assets/nodejs-project.zip . -i "out/*" "node_modules/*" "server/*" "server.js" "package.json" "config.json" -x "out/static/placeholder/*" "out/static/demo/*" "node_modules/node-moving-things-tracker/benchmark/*"
```

Full command

```bash
npm i;npm run build;npm run export;npm prune --production;rm ../opendatacam-mobile/android/app/src/main/assets/nodejs-project.zip;zip -0 -r ../opendatacam-mobile/android/app/src/main/assets/nodejs-project.zip . -i "out/*" "node_modules/*" "server/*" "server.js" "package.json" "config.json" -x "out/static/placeholder/*" "out/static/demo/*" "node_modules/node-moving-things-tracker/benchmark/*"
```

### Dev workflow

Open chrome, and open `chrome://inspect` , the webview should show up. 

PS: I have issue with latest version of chrome.. if doesn't work download a old version, for example this browser is based on a old version of chrome: https://www.slimjet.com 

#### diff for local development

use `/start` to start YOLO and then request again on `localhost:8080`


```javascript
──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
modified: config.json
──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
@ config.json:6 @
  "PATH_TO_YOLO_DARKNET" : "TO_REPLACE_PATH_TO_DARKNET",
  "CMD_TO_YOLO_DARKNET" : "TO_REPLACE_PATH_TO_DARKNET/darknet",
  "VIDEO_UPLOAD_FOLDER": "TO_REPLACE_PATH_TO_DARKNET/opendatacam_videos_uploaded",
  "VIDEO_INPUT": "TO_REPLACE_VIDEO_INPUT",
  "VIDEO_INPUT": "simulation",
  "NEURAL_NETWORK": "TO_REPLACE_NEURAL_NETWORK",
  "VIDEO_INPUTS_PARAMS": {
    "file": "opendatacam_videos/demo.mp4",
──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
modified: server.js
──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
@ server.js:37 @ if (packageJson.version !== config.OPENDATACAM_VERSION) {

const port = parseInt(process.env.PORT, 10) || configHelper.getAppPort();
const dev = process.env.NODE_ENV !== 'production';
const app = next({ dir: "/data/data/com.opendatacam/files/nodejs-project" })
const app = next({ dev  })
const handle = app.getRequestHandler();

// Log config loaded
@ server.js:68 @ if (config.VIDEO_INPUT === 'simulation') {
    };
  }
}
// const YOLO = new YoloDarknet(yoloConfig);
const YOLO = new YoloDarknet(yoloConfig);

// Select tracker, based on GPS settings in config
let tracker = Tracker;
──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
modified: server/db/DBManagerNeDB.js
──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
@ server/db/DBManagerNeDB.js:40 @ class DBManagerNeDB {
      });
    });

    this.db[RECORDING_COLLECTION] = new Datastore({ filename: '/data/data/com.opendatacam/files/opendatacam_recording.db' });
    this.db[TRACKER_COLLECTION] = new Datastore({ filename: '/data/data/com.opendatacam/files/opendatacam_tracker.db' });
    this.db[APP_COLLECTION] = new Datastore({ filename: '/data/data/com.opendatacam/files/opendatacam_app.db' });
    this.db[RECORDING_COLLECTION] = new Datastore({ filename: 'opendatacam_recording.db' });
    this.db[TRACKER_COLLECTION] = new Datastore({ filename: 'opendatacam_tracker.db' });
    this.db[APP_COLLECTION] = new Datastore({ filename: 'opendatacam_app.db' });


    await Promise.all([
```

### Troubleshooting

#### Infinite cmake loop on android build

```
# remove android/app/.cxx folder
rm -rf android/app/.cxx/
```

