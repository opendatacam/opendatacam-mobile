window.customElements.define('capacitor-welcome', class extends HTMLElement {
  constructor() {
    super();

    //Capacitor.Plugins.SplashScreen.hide();

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
        //window.location = "http://localhost:8080";
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

      .initializing-view.jsx-194310094{display:-webkit-box;display:-webkit-flex;display:-ms-flexbox;display:flex;-webkit-flex-direction:column;-ms-flex-direction:column;flex-direction:column;-webkit-align-items:center;-webkit-box-align:center;-ms-flex-align:center;align-items:center;-webkit-box-pack:center;-webkit-justify-content:center;-ms-flex-pack:center;justify-content:center;width:100%;height:100%;background-color:black;}.console.jsx-194310094{width:100%;-webkit-flex:1;-ms-flex:1;flex:1;}.progress-bar.jsx-194310094{min-width:200px;position:relative;}.progress-bar-content.jsx-194310094{content:'';position:absolute;top:0;left:0;width:100%;height:100%;-webkit-transform-origin:0 0;-ms-transform-origin:0 0;transform-origin:0 0;-webkit-transform:scaleX(0);-ms-transform:scaleX(0);transform:scaleX(0);}
      .main-page.jsx-1468007328{width:100%;height:100%;position:absolute;top:0;left:0;z-index:1;overflow:hidden;}
    
      .text-white {
        --text-opacity: 1;
        color: #fff;
        color: rgba(255,255,255,var(--text-opacity));
      }

      .text-3xl {
        font-size: 1.875rem;
      }

      .font-bold {
        font-weight: 700;
      }

      w-1/5 {
        width: 20%;
      } 
      
      .mt-5 {
        margin-top: 1.25rem;
      }
      
      .h-5 {
        height: 1.25rem;
      }

      </style>
    <div class="jsx-1468007328 main-page"><div class="jsx-194310094 initializing-view pt-20 pb-20 pr-12 pl-12"><h2 class="jsx-194310094 text-white text-3xl font-bold">Initializing OpenDataCam</h2><div class="jsx-194310094 w-1/5 mt-5 h-5 progress-bar rounded overflow-hidden"><div class="jsx-194310094 shadow w-full h-full bg-gray-900"><div class="jsx-194310094 bg-white py-2 progress-bar-content" style="transform: scaleX(0.610467);"></div></div></div><button class="jsx-194310094 btn btn-light mt-10 rounded">Show details</button></div></div>
    `
  }
});
