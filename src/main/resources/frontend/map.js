window.loadMap = function () {
    Loader.async = true
    Loader.load(null, null, createMap)
}

let map;
let markersLayer;
let geometryLayer;

let rootUrl = window.location.host

let ssl = (window.location.protocol === "https:")
let wsUrl = "ws" + (ssl ? "s" : "") + "://" + rootUrl + "/subscribe";

console.log(wsUrl)

let lineColor = "#f00"

let wsSocket = new WebSocket(wsUrl)

wsSocket.addEventListener('open', function (event) {
    console.log('Server WS connected!');
});

let lastCoords;
let lastPosMarker;

window.createMap = function () {
    const queryParams = new Proxy(new URLSearchParams(window.location.search), {
        get: (searchParams, prop) => searchParams.get(prop),
    })
    let trackId = queryParams.track

    if (trackId === undefined) {
        alert("Není vybrán žádný track!");
    }

    const xhttp = new XMLHttpRequest();
    xhttp.onload = function () {
        let xmlDoc = JAK.XML.createDocument(this.responseText)

        let pts = xmlDoc.getElementsByTagName("trkpt")
        let firstPoint = pts[0];
        let lastPoint = pts[pts.length - 1];

        // init map
        let center = SMap.Coords.fromWGS84(lastPoint.getAttribute("lon"), lastPoint.getAttribute("lat"))
        map = new SMap(JAK.gel("map"), center, 13)
        map.addDefaultLayer(SMap.DEF_TURIST).enable()
        map.addDefaultControls()

        let sync = new SMap.Control.Sync({})
        map.addControl(sync)

        map.addDefaultLayer(SMap.DEF_TURIST).enable()
        map.addDefaultLayer(SMap.DEF_OPHOTO)
        map.addDefaultLayer(SMap.DEF_BASE)

        let layerSwitch = new SMap.Control.Layer({width: 65, items: 3, page: 3})
        layerSwitch.addDefaultLayer(SMap.DEF_BASE)
        layerSwitch.addDefaultLayer(SMap.DEF_OPHOTO)
        layerSwitch.addDefaultLayer(SMap.DEF_TURIST)
        map.addControl(layerSwitch, {left: "8px", top: "40px"})

        markersLayer = new SMap.Layer.Marker()
        map.addLayer(markersLayer)
        markersLayer.enable()

        geometryLayer = new SMap.Layer.Geometry()
        map.addLayer(geometryLayer)
        geometryLayer.enable()

        // draw all waypoints
        let wpts = xmlDoc.getElementsByTagName("wpt")
        for (let i = 0; i < wpts.length; i++) {
            let wpt = wpts[i]

            let name = wpt.getElementsByTagName("name")[0].innerHTML

            let lat = wpt.getAttribute("lat")
            let lon = wpt.getAttribute("lon")
            let visited = wpt.getAttribute("visited")
            let color = visited === "true" ? "blue" : "red"
            let img = SMap.CONFIG.img + "/marker/drop-" + color + ".png"

            markersLayer.addMarker(makeMarker(lat, lon, (i + 1).toString(), name, img))
        }

        // remove waypoints so they're not rendered twice
        while (wpts.length > 0) {
            let wpt = wpts[0]
            wpt.parentNode.removeChild(wpt)
        }

        // waypoint representing current position
        lastPosMarker = makeMarkerFromTrackpoint(lastPoint);
        markersLayer.addMarker(lastPosMarker)

        lastCoords = SMap.Coords.fromWGS84(lastPoint.getAttribute("lon"), lastPoint.getAttribute("lat"))

        // pass the rest to draw the line
        let gpx = new SMap.Layer.GPX(xmlDoc, null, {maxPoints: 5000, colors: [lineColor]})
        map.addLayer(gpx)
        gpx.enable()
        gpx.fit()

        wsSocket.addEventListener('message', function (event) {
            if (event.data.startsWith("!!")) {
                console.log('Message from server ' + event.data);
                alert("WS: " + event.data)
                return
            }

            let coords = JSON.parse(event.data)
            console.log("Received new coords: " + JSON.stringify(coords))

            let currentCoords = SMap.Coords.fromWGS84(coords.lon, coords.lat)

            let points1 = [
                currentCoords,
                lastCoords
            ]
            let options1 = {
                color: lineColor,
                width: 4
            }
            let polyline = new SMap.Geometry(SMap.GEOMETRY_POLYLINE, null, points1, options1)
            geometryLayer.addGeometry(polyline)

            // TODO change text too
            lastPosMarker.setCoords(currentCoords)

            lastCoords = currentCoords

        });

        wsSocket.send("coordinates/" + trackId)
    }

    xhttp.open("GET", window.location.protocol + "//" + rootUrl + "/list/gpx/" + trackId, true)
    xhttp.send()
}

function makeMarker(lat, lon, title, text, img) {
    let card = new SMap.Card()
    card.getHeader().innerHTML = "<strong>" + text + "</strong>"
    card.getBody().innerHTML = ""

    let markerContent = JAK.mel("div")
    let pic = JAK.mel("img", {src: img})
    markerContent.appendChild(pic)

    let markerTitle = JAK.mel("div", {}, {
        position: "absolute",
        left: "0px",
        top: "2px",
        textAlign: "center",
        width: "22px",
        color: "white",
        fontWeight: "bold"
    })
    markerTitle.innerHTML = title
    markerContent.appendChild(markerTitle)

    let coords = SMap.Coords.fromWGS84(lon, lat);
    let marker = new SMap.Marker(coords, null, {url: markerContent});
    marker.decorate(SMap.Marker.Feature.Card, card)
    return marker
}

function makeMarkerFromTrackpoint(trkpt) {
    let time = trkpt.getElementsByTagName("time")[0].innerHTML

    let lat = trkpt.getAttribute("lat")
    let lon = trkpt.getAttribute("lon")
    let batt = trkpt.getAttribute("batt")

    console.log("Last position: time " + time + " lat=" + lat + ", lon=" + lon + ", batt=" + batt)

    let marker = makeMarker(lat, lon, "", time + ", batt: " + batt, "/walking_icon.png")

    // TODO anchor to center
    // let options = {anchor: {left: 0.5, top: 0.5}}
    // marker.decorate(SMap.Marker.Feature.RelativeAnchor, options)

    return marker
}
