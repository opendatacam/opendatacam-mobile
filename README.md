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

# build front-end code
npm i
npm run build

# prune node_module from dev dependencies
npm prune --production

# pack as a zip (ignore useless folder) and put in assets folder of the mobile project

# delete previous zip
rm ../opendatacam-mobile/android/app/src/main/assets/nodejs-project.zip

# zip new one
zip -0 -r ../opendatacam-mobile/android/app/src/main/assets/nodejs-project.zip . -x ".git/*" ".github/*" "public/static/placeholder/*" "public/static/demo/*" "documentation/*" ".next/*" "apidoc/*" "docker/*" "script/*" "spec/*"
```

Full command

```bash
npm i;npm run build;npm prune --production;rm ../opendatacam-mobile/android/app/src/main/assets/nodejs-project.zip;zip -0 -r ../opendatacam-mobile/android/app/src/main/assets/nodejs-project.zip . -x ".git/*" ".github/*" "public/static/placeholder/*" "public/static/demo/*" "documentation/*" ".next/*" "apidoc/*" "docker/*" "script/*" "spec/*";
```

### Dev workflow

Open chrome, and open `chrome://inspect` , the webview should show up. 

PS: I have issue with latest version of chrome.. if doesn't work download a old version, for example this browser is based on a old version of chrome: https://www.slimjet.com 

### Troubleshooting

#### Infinite cmake loop on android build

```
# remove android/app/.cxx folder
rm -rf android/app/.cxx/
```

