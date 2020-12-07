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
      data.map((object) => {
        ctx.fillRect(object.x0, object.y0,object.x1 -  object.x0, object.y1 -  object.y0);
      })
      console.log(data);
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
    <div>
      <canvas id="myCanvas"></canvas>
    </div>
    `
  }
});
