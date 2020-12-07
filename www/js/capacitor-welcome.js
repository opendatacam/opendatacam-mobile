window.customElements.define('capacitor-welcome', class extends HTMLElement {
  constructor() {
    super();

    Capacitor.Plugins.SplashScreen.hide();

    Capacitor.Plugins.CameraObjectDetection.startObjectDetection();

    Capacitor.Plugins.CameraObjectDetection.addListener("frameData", (frameData) => {
      var data = JSON.parse(frameData.frameData);
      var canvas = this.shadowRoot.getElementById("myCanvas");
      var ctx = canvas.getContext("2d");
      ctx.fillStyle = "#FF0000";
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      data.map((object) => {
        var objectWidth = object.x1 -  object.x0;
        var objectHeight = object.y1 -  object.y0;
        var scaledObject = {
          x: canvas.width * object.x0 / 480,
          y: canvas.height * object.y0 / 640,
          width: canvas.width * objectWidth / 480,
          height: canvas.height * objectHeight / 640
        }
        ctx.fillRect(scaledObject.x, scaledObject.y,scaledObject.width, scaledObject.height);
      })
    })

    const root = this.attachShadow({ mode: 'open' });

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
