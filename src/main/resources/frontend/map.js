window.loadMap = function () {
    Loader.async = true
    Loader.load(null, null, createMap)
}

let map;
let markersLayer;

window.createMap = function () {
    let center = SMap.Coords.fromWGS84(14.41790, 50.12655)
    map = new SMap(JAK.gel("map"), center, 13)
    map.addDefaultLayer(SMap.DEF_TURIST).enable()
    map.addDefaultControls()

    let sync = new SMap.Control.Sync({})
    map.addControl(sync)

    map.addDefaultLayer(SMap.DEF_OPHOTO);
    map.addDefaultLayer(SMap.DEF_TURIST);
    map.addDefaultLayer(SMap.DEF_BASE).enable();

    let layerSwitch = new SMap.Control.Layer({width: 65, items: 4, page: 4});
    layerSwitch.addDefaultLayer(SMap.DEF_BASE);
    layerSwitch.addDefaultLayer(SMap.DEF_OPHOTO);
    layerSwitch.addDefaultLayer(SMap.DEF_TURIST);
    layerSwitch.addDefaultLayer(SMap.DEF_TURIST_WINTER);
    layerSwitch.addDefaultLayer(SMap.DEF_OPHOTO0406);
    layerSwitch.addDefaultLayer(SMap.DEF_OPHOTO0203);
    layerSwitch.addDefaultLayer(SMap.DEF_HISTORIC);
    map.addControl(layerSwitch, {left: "8px", top: "40px"});

    markersLayer = new SMap.Layer.Marker()
    map.addLayer(markersLayer)
    markersLayer.enable()

    const queryParams = new Proxy(new URLSearchParams(window.location.search), {
        get: (searchParams, prop) => searchParams.get(prop),
    })
    let trackerId = queryParams.tracker

    if (trackerId === undefined) {
        alert("Není vybrán žádný tracker!");
    }

    const xhttp = new XMLHttpRequest();
    xhttp.onload = function () {
        let xmlDoc = JAK.XML.createDocument(this.responseText)

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
        let pts = xmlDoc.getElementsByTagName("trkpt")
        markersLayer.addMarker(makeMarkerFromTrackpoint(pts[pts.length - 1]))

        // pass the rest to draw the line
        let gpx = new SMap.Layer.GPX(xmlDoc)
        map.addLayer(gpx)
        gpx.enable()
        gpx.fit()
    }
    xhttp.open("GET", "/list/gpx/" + trackerId, true)
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
