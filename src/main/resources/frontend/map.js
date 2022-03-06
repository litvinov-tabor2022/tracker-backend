window.loadMap = function () {
    Loader.async = true
    Loader.load(null, null, createMap)
}

window.createMap = function () {
    let center = SMap.Coords.fromWGS84(14.41790, 50.12655)
    let map = new SMap(JAK.gel("map"), center, 13)
    map.addDefaultLayer(SMap.DEF_TURIST).enable()
    map.addDefaultControls()

    let sync = new SMap.Control.Sync({bottomSpace: 30})
    map.addControl(sync)

    let markersLayer = new SMap.Layer.Marker()
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

        let wpts = xmlDoc.getElementsByTagName("wpt")

        for (let i = 0; i < wpts.length; i++) {
            let wpt = wpts[i]

            let name = wpt.getElementsByTagName("name")[0].innerHTML

            let lat = wpt.getAttribute("lat")
            let lon = wpt.getAttribute("lon")
            let visited = wpt.getAttribute("visited")

            let card = new SMap.Card()
            card.getHeader().innerHTML = "<strong>"+ name +"</strong>"
            card.getBody().innerHTML = ""

            let markerContent = JAK.mel("div")
            let pic = JAK.mel("img", {src:SMap.CONFIG.img+"/marker/drop-"+(visited === "true" ? "blue":"red")+".png"})
            markerContent.appendChild(pic)

            let markerTitle = JAK.mel("div", {}, {position:"absolute", left:"0px", top:"2px", textAlign:"center", width:"22px", color:"white", fontWeight:"bold"})
            markerTitle.innerHTML = (i+1).toString()
            markerContent.appendChild(markerTitle)

            let coords = SMap.Coords.fromWGS84(lon, lat);
            let marker = new SMap.Marker(coords, null, {url:markerContent});
            marker.decorate(SMap.Marker.Feature.Card, card)
            markersLayer.addMarker(marker)
        }

        // remove so it's not rendered twice
        while (wpts.length > 0) {
            let wpt = wpts[0]
            wpt.parentNode.removeChild(wpt)
        }

        let gpx = new SMap.Layer.GPX(xmlDoc)
        map.addLayer(gpx)
        gpx.enable()
        gpx.fit()
    }
    xhttp.open("GET", "http://localhost:8080/list/gpx/" + trackerId, true)
    xhttp.send()
}
