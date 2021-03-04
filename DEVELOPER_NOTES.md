## Developer notes

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

#### diff for local development

use `/start` to start YOLO and then request again on `localhost:8080`

Apply this patch: https://github.com/opendatacam/opendatacam/blob/mobile/mobile-branch-dev.patch

```
git apply mobile-branch-dev.patch    
```

This will:

- renable next() on node.js side to serve front-end
- change paths of NeDB location
- renable YOLOSimulation
- Mock cameraLocation
- Display <WebcamStream> component in MainPage.js (when on android device we do not render it as the native code render the camera view)

_To generate the patch `git diff > mobile-branch-dev.patch`, and then remove the part in the patch that create a diff of the patch itself..._

### Troubleshooting

#### Infinite cmake loop on android build

```
# remove android/app/.cxx folder
rm -rf android/app/.cxx/
```

### External Dependencies

- Neural network inference framework NCNN : https://github.com/Tencent/ncnn (LICENSE BSD 3-Clause)
- Nodejs mobile : https://github.com/JaneaSystems/nodejs-mobile (LICENSE MIT)

