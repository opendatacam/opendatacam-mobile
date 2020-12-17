## OpenDataCam Mobile

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
zip -0 -r ../opendatacam-mobile/android/app/src/main/assets/nodejs-project.zip . -x ".git/*" ".github/*" "public/static/placeholder/*" "public/static/demo/*" "documentation/*" ".next/*" "apidoc/*" "docker/*" "script/*" "spec/*"
```


