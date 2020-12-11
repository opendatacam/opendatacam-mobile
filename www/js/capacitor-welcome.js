window.customElements.define('capacitor-welcome', class extends HTMLElement {
  constructor() {
    super();

    Capacitor.Plugins.SplashScreen.hide();

    //Capacitor.Plugins.CameraObjectDetection.startObjectDetection();

    /*
    Capacitor.Plugins.CameraObjectDetection.addListener("frameData", (frameData) => {
      var data = JSON.parse(frameData.frameData);
      var canvas = this.shadowRoot.getElementById("myCanvas");
      var ctx = canvas.getContext("2d");
      ctx.fillStyle = "#FF0000";
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      data.map((object) => {
        var scaledObject = {
          x: canvas.width * object.x,
          y: canvas.height * object.y,
          width: canvas.width * object.width ,
          height: canvas.height * object.height
        }
        ctx.fillRect(scaledObject.x, scaledObject.y,scaledObject.width, scaledObject.height);
      })
    }) */

    const root = this.attachShadow({ mode: 'open' });

    var startOpenDataCam = setInterval(() => {
      // Request opendatacam app until the node.js server is started
      fetch('http://localhost:8080', {mode: "no-cors"}).then(function(response) {
        console.log('Started, yeah !');
        window.location = "http://localhost:8080";
        clearInterval(startOpenDataCam);
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

      #myCanvas {
        width: 100%;
        height: 100%;
      }
      
    </style>
    <canvas id="myCanvas"></canvas>
    `
  }
});
