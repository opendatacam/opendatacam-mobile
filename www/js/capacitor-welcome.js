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

    window.location = "http://localhost:8080";

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
