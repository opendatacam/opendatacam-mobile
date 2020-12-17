window.customElements.define('capacitor-welcome', class extends HTMLElement {
  constructor() {
    super();

    Capacitor.Plugins.SplashScreen.hide();

    const root = this.attachShadow({ mode: 'open' });

    // only to ask permissions
    //Capacitor.Plugins.CameraObjectDetection.startObjectDetection();
    
    Capacitor.Plugins.CameraObjectDetection.addListener("frameData", (frameData) => {
      var data = JSON.parse(frameData.frameData);

      
      // Format data like the YOLO JSON return
        /*
        {
          "frame_id":16, 
          "objects": [ 
          {"class_id":6, "name":"car", "relative_coordinates":{"center_x":0.485693, "center_y":0.570632, "width":0.091478, "height":0.156059}, "confidence":0.892377}
          ] 
        } */

      var frameUpdate = {
        frame_id: frameId,
        objects: data.map((object) => {
          return {
            class_id: "6",
            name: "car",
            relative_coordinates: {
              center_x: object.x + object.width / 2,
              center_y: object.y + object.height / 2,
              width: object.width,
              height: object.height
            },
            confidence: 0.8
          }
        })
      }


      // update this detection to the node.js app
      fetch('http://localhost:8080/updatewithnewframe', {
        method: 'POST',
        mode: "no-cors",
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(frameUpdate)
      })    
    })

    var startOpenDataCam = setInterval(() => {
      // Request opendatacam app until the node.js server is started
      fetch('http://localhost:8080', {mode: "no-cors"}).then(function(response) {
        console.log('Started, yeah !');
        window.location = "http://localhost:8080";
        clearInterval(startOpenDataCam);
        // here then the link with the capacitor is lost.. obviously
      })
    }, 1000);

    root.innerHTML = `
    <style>
      :host {
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol";
        display: block;
        width: 100%;
        height: 100%;
      }
    </style>
    <h1>Starting Node.js server...</h1>
    `
  }
});
