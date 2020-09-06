package ar.edu.unahur.obj2.servidorWeb

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class ServidorWebTest : DescribeSpec({
  describe("Un servidor web") {
    val servidor = ServidorWeb()
    servidor.agregarModulo(
      Modulo(listOf("txt"), "todo bien", 100)
    )
    servidor.agregarModulo(
      Modulo(listOf("jpg", "gif"), "qué linda foto", 100)
    )
    servidor.agregarModulo(
      Modulo(listOf("docx", "odt"), "documento a la vista", 100)
    )

    it("devuelve 501 si recibe un pedido que no es HTTP") {
      val respuesta = servidor.realizarPedido("207.46.13.5", "https://pepito.com.ar/hola.txt", LocalDateTime.now())
      respuesta.codigo.shouldBe(CodigoHttp.NOT_IMPLEMENTED)
      respuesta.body.shouldBe("")
      respuesta.tiempo.shouldBe(10)
    }

    it("devuelve 200 si algún módulo puede trabajar con el pedido") {
      val respuesta = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/hola.txt", LocalDateTime.now())
      respuesta.codigo.shouldBe(CodigoHttp.OK)
      respuesta.body.shouldBe("todo bien")
      respuesta.tiempo.shouldBe(100)
    }

    it("devuelve 404 si ningún módulo puede trabajar con el pedido") {
      val respuesta = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/playa.png", LocalDateTime.now())
      respuesta.codigo.shouldBe(CodigoHttp.NOT_FOUND)
      respuesta.body.shouldBe("")
      respuesta.tiempo.shouldBe(10)
    }

    it("devuelve 200 si ningún módulo puede trabajar con el pedido y es un documento") {
      val respuesta = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/secreto.docx", LocalDateTime.now())
      respuesta.codigo.shouldBe(CodigoHttp.OK)
      respuesta.body.shouldBe("documento a la vista")
      respuesta.tiempo.shouldBe(100)
    }

    // Analizadores

    val analizadorDemora = AnalizadorDeDemora(demoraMinima = 50)
    val analizadorIPs = AnalizadorDeIP()
    val analizadorEstadisticas = AnalizadorEstadistica()
    analizadorIPs.agregar("201.11.0.88")
    analizadorIPs.agregar("200.51.101.1")
    analizadorIPs.agregar("151.21.31.2")

    val respuesta1 = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/secreto.docx", LocalDateTime.now())
    val respuesta2 = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/playa.jpg", LocalDateTime.now())

    it("El servidor recibe respuestas y las envia a los analizadores, como no hay ningun no pasa nada") {
      servidor.enviarALosAnalizadores(respuesta1)
      servidor.enviarALosAnalizadores(respuesta2)
    }

    // Analizador de Demora
    it("El servidor recibe respuestas y las envia a los analizadores: analizador de demora") {
      val analizadorDemora2 = AnalizadorDeDemora(demoraMinima = 105)
      // Se agrega analizador y respuestas a este
      servidor.agregarAnalizador(analizadorDemora)
      servidor.enviarALosAnalizadores(respuesta1)
      servidor.enviarALosAnalizadores(respuesta2)
      analizadorDemora.hayDemora().shouldBeTrue()
      // Se quita analizador
      servidor.quitarAnalizador(analizadorDemora)
      // Se agrega un analizador con mayor demoraMinima
      servidor.agregarAnalizador(analizadorDemora2)
      servidor.enviarALosAnalizadores(respuesta1)
      servidor.enviarALosAnalizadores(respuesta2)
      analizadorDemora2.hayDemora().shouldBeFalse()
      // Se le pregunta al servidor, para un módulo, la cantidad de respuestas demoradas.
      val moduloPY = Modulo(listOf("py", "ipynb"), "Archivos python", 120)
      servidor.agregarModulo(moduloPY)
      val respuesta3 = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/codigo.py", LocalDateTime.now())
      respuesta3.codigo.shouldBe(CodigoHttp.OK)
      respuesta3.body.shouldBe("Archivos python")
      respuesta3.tiempo.shouldBe(120)
      val respuesta4 = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/fuente.py", LocalDateTime.now())
      servidor.enviarALosAnalizadores(respuesta3)
      servidor.enviarALosAnalizadores(respuesta4)
      analizadorDemora2.cantidadDeDemoras(moduloPY).shouldBe(2)
    }

    // Analizador IPs Sospechosas


    it("El servidor recibe respuestas y las envia a los analizadores: IPs sospechosa ") {
      val analizadorIPs1 = AnalizadorDeIP()
      analizadorIPs1.agregar("201.11.0.88")
      analizadorIPs1.agregar("200.51.101.1")
      analizadorIPs1.agregar("151.21.31.2")
      //val respuesta1 = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/secreto.docx", LocalDateTime.now())
      //val respuesta2 = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/playa.jpg", LocalDateTime.now())
      val respuesta3 = servidor.realizarPedido("200.51.101.1", "http://pepito.com.ar/secreto.docx", LocalDateTime.now())
      val respuesta4 = servidor.realizarPedido("200.51.101.1", "http://pepito.com.ar/Password.txt", LocalDateTime.now())
      val respuesta5 = servidor.realizarPedido("201.11.0.88", "http://pepito.com.ar/pirata.docx", LocalDateTime.now())

      servidor.agregarAnalizador(analizadorIPs1)
      servidor.agregarAnalizador(analizadorDemora)
      servidor.enviarALosAnalizadores(respuesta1)
      servidor.enviarALosAnalizadores(respuesta2)
      servidor.enviarALosAnalizadores(respuesta3)
      servidor.enviarALosAnalizadores(respuesta4)
      servidor.enviarALosAnalizadores(respuesta5)

      // cuántos pedidos realizó una cierta IP sospechosa
      analizadorIPs1.pedidosSospechosos("200.51.101.1").shouldBe(2)

      // cuál es el módulo más consultado por todas las IPs sospechosas
      val moduloPY = Modulo(listOf("py", "ipynb"), "Archivos python", 120)
      servidor.agregarModulo(moduloPY)
      val respuesta6 = servidor.realizarPedido("201.11.0.88", "http://pepito.com.ar/coyigo.py", LocalDateTime.now())
      val respuesta7 = servidor.realizarPedido("201.11.0.88", "http://pepito.com.ar/cuiyigo.py", LocalDateTime.now())
      val respuesta8 = servidor.realizarPedido("201.11.0.88", "http://pepito.com.ar/propertys.py", LocalDateTime.now())
      servidor.enviarALosAnalizadores(respuesta6)
      servidor.enviarALosAnalizadores(respuesta7)
      servidor.enviarALosAnalizadores(respuesta8)
      analizadorIPs1.cuantasVecesConsultaronLasIpSospechosas(servidor).shouldBe(moduloPY)
    }

    it("El servidor recibe respuestas y las envia a los analizadores: IPs sospechosa version2") {
      val analizadorIPs1 = AnalizadorDeIP()
      analizadorIPs1.agregar("201.11.0.88")
      analizadorIPs1.agregar("200.51.101.1")
      analizadorIPs1.agregar("151.21.31.2")
      //val respuesta1 = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/secreto.docx", LocalDateTime.now())
      //val respuesta2 = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/playa.jpg", LocalDateTime.now())
      val respuesta3 = servidor.realizarPedido("200.51.101.1", "http://pepito.com.ar/secreto.docx", LocalDateTime.now())
      val respuesta4 = servidor.realizarPedido("200.51.101.1", "http://pepito.com.ar/Password.txt", LocalDateTime.now())
      val respuesta5 = servidor.realizarPedido("201.11.0.88", "http://pepito.com.ar/pirata.docx", LocalDateTime.now())

      servidor.agregarAnalizador(analizadorIPs1)
      servidor.agregarAnalizador(analizadorDemora)
      servidor.enviarALosAnalizadores(respuesta1)
      servidor.enviarALosAnalizadores(respuesta2)
      servidor.enviarALosAnalizadores(respuesta3)
      servidor.enviarALosAnalizadores(respuesta4)
      servidor.enviarALosAnalizadores(respuesta5)

      // cuántos pedidos realizó una cierta IP sospechosa
      analizadorIPs1.pedidosSospechosos("200.51.101.1").shouldBe(2)

      // cuál es el módulo más consultado por todas las IPs sospechosas
      val moduloPY = Modulo(listOf("py", "ipynb"), "Archivos python", 120)
      servidor.agregarModulo(moduloPY)
      val respuesta6 = servidor.realizarPedido("201.11.0.88", "http://pepito.com.ar/coyigo.py", LocalDateTime.now())
      val respuesta7 = servidor.realizarPedido("201.11.0.88", "http://pepito.com.ar/cuiyigo.py", LocalDateTime.now())
      val respuesta8 = servidor.realizarPedido("201.11.0.88", "http://pepito.com.ar/propertys.py", LocalDateTime.now())
      servidor.enviarALosAnalizadores(respuesta6)
      servidor.enviarALosAnalizadores(respuesta7)
      servidor.enviarALosAnalizadores(respuesta8)
      analizadorIPs1.cuantasVecesConsultaronLasIpSospechosas(servidor).shouldBe(moduloPY)

      // cuál es el módulo más consultado por todas las IPs sospechosas: se agrega otro modulo, con mas consultas sospechosas
      val moduloASP = Modulo(listOf("asp", "html"), "Archivos web", 120)
      servidor.agregarModulo(moduloASP)
      val respuesta9 = servidor.realizarPedido("151.21.31.2", "http://pepito.com.ar/coyigo.asp", LocalDateTime.now())
      val respuesta10 = servidor.realizarPedido("151.21.31.2", "http://pepito.com.ar/cuiyigo.asp", LocalDateTime.now())
      val respuesta11 =
        servidor.realizarPedido("151.21.31.2", "http://pepito.com.ar/propertys.asp", LocalDateTime.now())
      val respuesta12 =
        servidor.realizarPedido("151.21.31.2", "http://pepito.com.ar/propertys.asp", LocalDateTime.now())
      servidor.enviarALosAnalizadores(respuesta9)
      servidor.enviarALosAnalizadores(respuesta10)
      servidor.enviarALosAnalizadores(respuesta11)
      servidor.enviarALosAnalizadores(respuesta12)
      analizadorIPs1.cuantasVecesConsultaronLasIpSospechosas(servidor).shouldBe(moduloASP)
    }

    it("El servidor recibe respuestas y las envia a los analizadores: IPs sospechosa version3") {
      val analizadorIPs1 = AnalizadorDeIP()
      analizadorIPs1.agregar("201.11.0.88")
      analizadorIPs1.agregar("200.51.101.1")
      analizadorIPs1.agregar("151.21.31.2")
      //val respuesta1 = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/secreto.docx", LocalDateTime.now())
      //val respuesta2 = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/playa.jpg", LocalDateTime.now())
      val respuesta3 = servidor.realizarPedido("200.51.101.1", "http://youporn.com/secreto.docx", LocalDateTime.now())
      val respuesta4 = servidor.realizarPedido("200.51.101.1", "http://youporn.com/Password.docx", LocalDateTime.now())
      val respuesta5 = servidor.realizarPedido("201.11.0.88", "http://youporn.com/pirata.docx", LocalDateTime.now())

      servidor.agregarAnalizador(analizadorIPs1)
      servidor.agregarAnalizador(analizadorDemora)

      servidor.enviarALosAnalizadores(respuesta1)
      servidor.enviarALosAnalizadores(respuesta2)
      servidor.enviarALosAnalizadores(respuesta3)
      servidor.enviarALosAnalizadores(respuesta4)
      servidor.enviarALosAnalizadores(respuesta5)

      // conjunto de IPs sospechosas que requirieron una cierta ruta.
      analizadorIPs1.ipSospechosasEnLaRuta("http://youporn.com/").shouldBe(setOf("200.51.101.1","201.11.0.88"))
    }

    // Analizador estadisticas

    it("Tiempo de respuesta promedio: Analizador de estadisticas") {
      // modulos que ya estan cargados:
      // Modulo(listOf("txt"), "todo bien", 100)
      // Modulo(listOf("jpg", "gif"), "qué linda foto", 100)
      // Modulo(listOf("docx", "odt"), "documento a la vista", 100)
      // definiendo modulos adicionales
      val moduloPY = Modulo(listOf("py", "ipynb"), "Archivos python", 120)
      servidor.agregarModulo(moduloPY)
      val moduloASP = Modulo(listOf("asp", "html"), "Archivos web", 120)
      servidor.agregarModulo(moduloASP)
      // agregando analizador de estadistica
      servidor.agregarAnalizador(analizadorEstadisticas)
      // enviando mensajes a los analizadores
      servidor.enviarALosAnalizadores(respuesta1)
      servidor.enviarALosAnalizadores(respuesta2)
      // promedio de tiempos de respuesta
      analizadorEstadisticas.tiemposDeRespuesta(servidor)shouldBe(108)

      // agregando otro modulo y otros analizadores, incluyendo otro analizador de estadistica.
      val moduloPRG = Modulo(listOf("prg"), "Archivos prg", 120)
      servidor.agregarModulo(moduloPRG)
      servidor.agregarAnalizador(analizadorDemora)
      servidor.agregarAnalizador(analizadorIPs)
      val analizadorEstadisticas2 = AnalizadorEstadistica()
      servidor.agregarAnalizador(analizadorEstadisticas2)
      analizadorEstadisticas2.tiemposDeRespuesta(servidor).shouldBe(110)
    }

    it("cantidad de pedidos entre dos momentos (fecha/hora): Analizador de estadisticas") {
      // modulos que ya estan cargados:
      // Modulo(listOf("txt"), "todo bien", 100)
      // Modulo(listOf("jpg", "gif"), "qué linda foto", 100)
      // Modulo(listOf("docx", "odt"), "documento a la vista", 100)
      // definiendo modulos adicionales
      val moduloPY = Modulo(listOf("py", "ipynb"), "Archivos python", 120)
      servidor.agregarModulo(moduloPY)
      val moduloASP = Modulo(listOf("asp", "html"), "Archivos web", 120)
      servidor.agregarModulo(moduloASP)
      // agregando analizador de estadistica
      servidor.agregarAnalizador(analizadorEstadisticas)
      // enviando mensajes a los analizadores
      val respuesta3 = servidor.realizarPedido("200.51.101.1", "http://youporn.com/secreto.docx", LocalDateTime.parse("2020-08-30T09:55:00"))
      val respuesta4 = servidor.realizarPedido("200.51.101.1", "http://youporn.com/Password.docx", LocalDateTime.parse("2020-08-31T09:00:00"))
      val respuesta5 = servidor.realizarPedido("201.11.0.88", "http://youporn.com/pirata.docx", LocalDateTime.parse("2020-08-31T10:00:00"))
      val respuesta6 = servidor.realizarPedido("201.11.0.88", "http://youporn.com/pirata.docx", LocalDateTime.parse("2020-09-01T10:00:00"))
      val respuesta7 = servidor.realizarPedido("201.11.0.88", "http://youporn.com/pirata.docx", LocalDateTime.parse("2020-09-02T10:00:00"))
      servidor.enviarALosAnalizadores(respuesta1)
      servidor.enviarALosAnalizadores(respuesta2)
      servidor.enviarALosAnalizadores(respuesta3)
      servidor.enviarALosAnalizadores(respuesta4)
      servidor.enviarALosAnalizadores(respuesta5)
      servidor.enviarALosAnalizadores(respuesta6)
      servidor.enviarALosAnalizadores(respuesta7)
      // cantidad de pedidos entre dos momentos (fecha/hora)
      //servidor.cantidadPedidosEntre(LocalDateTime.parse("2020-08-31T01:00:00"),LocalDateTime.parse("2020-09-01T23:00:00")).shouldBe(3)
      analizadorEstadisticas.cantPedidosEntre(LocalDateTime.parse("2020-08-31T01:00:00"),LocalDateTime.parse("2020-09-01T23:00:00")).shouldBe(3)
    }

    it(" cantidad de respuestas cuyo body incluye un determinado String: Analizador de estadisticas") {
      // modulos que ya estan cargados:
      // Modulo(listOf("txt"), "todo bien", 100)
      // Modulo(listOf("jpg", "gif"), "qué linda foto", 100)
      // Modulo(listOf("docx", "odt"), "documento a la vista", 100)
      // definiendo modulos adicionales
      val moduloPY = Modulo(listOf("py", "ipynb"), "Archivos python", 120)
      servidor.agregarModulo(moduloPY)
      val moduloASP = Modulo(listOf("asp", "html"), "Archivos web", 120)
      servidor.agregarModulo(moduloASP)
      // agregando analizador de estadistica
      servidor.agregarAnalizador(analizadorEstadisticas)
      // enviando mensajes a los analizadores
      val respuesta3 = servidor.realizarPedido("200.51.101.1", "http://youporn.com/secreto.docx", LocalDateTime.parse("2020-08-30T09:55:00"))
      val respuesta4 = servidor.realizarPedido("200.51.101.1", "http://youporn.com/Password.py", LocalDateTime.parse("2020-08-31T09:00:00"))
      val respuesta5 = servidor.realizarPedido("201.11.0.88", "http://youporn.com/pirata.py", LocalDateTime.parse("2020-08-31T10:00:00"))
      val respuesta6 = servidor.realizarPedido("201.11.0.88", "http://youporn.com/pirata.docx", LocalDateTime.parse("2020-09-01T10:00:00"))
      val respuesta7 = servidor.realizarPedido("201.11.0.88", "http://youporn.com/pirata.py", LocalDateTime.parse("2020-09-02T10:00:00"))
      servidor.enviarALosAnalizadores(respuesta1)
      servidor.enviarALosAnalizadores(respuesta2)
      servidor.enviarALosAnalizadores(respuesta3)
      servidor.enviarALosAnalizadores(respuesta4)
      servidor.enviarALosAnalizadores(respuesta5)
      servidor.enviarALosAnalizadores(respuesta6)
      servidor.enviarALosAnalizadores(respuesta7)
      // cantidad de respuestas cuyo body incluye un determinado String: "Archivos python"
      analizadorEstadisticas.cantRespuestasConElBody("Archivos python").shouldBe(3)
      // cantidad de respuestas cuyo body incluye un determinado String: "python"
      analizadorEstadisticas.cantRespuestasConElBody("python").shouldBe(3)
    }

    it("porcentaje de pedidos con respuesta exitosa: Analizador de estadisticas") {
      // modulos que ya estan cargados:
      // Modulo(listOf("txt"), "todo bien", 100)
      // Modulo(listOf("jpg", "gif"), "qué linda foto", 100)
      // Modulo(listOf("docx", "odt"), "documento a la vista", 100)
      // definiendo modulos adicionales
      val moduloPY = Modulo(listOf("py", "ipynb"), "Archivos python", 120)
      servidor.agregarModulo(moduloPY)
      val moduloASP = Modulo(listOf("asp", "html"), "Archivos web", 120)
      servidor.agregarModulo(moduloASP)
      // agregando analizador de estadistica
      servidor.agregarAnalizador(analizadorEstadisticas)
      // enviando mensajes a los analizadores
      val respuesta3 = servidor.realizarPedido("200.51.101.1", "http://youporn.com/secreto.docx", LocalDateTime.parse("2020-08-30T09:55:00"))
      val respuesta4 = servidor.realizarPedido("200.51.101.1", "http://youporn.com/Password.py", LocalDateTime.parse("2020-08-31T09:00:00"))
      val respuesta5 = servidor.realizarPedido("201.11.0.88", "http://youporn.com/pirata.py", LocalDateTime.parse("2020-08-31T10:00:00"))
      val respuesta6 = servidor.realizarPedido("201.11.0.88", "http://youporn.com/pirata.docx", LocalDateTime.parse("2020-09-01T10:00:00"))
      val respuesta7 = servidor.realizarPedido("201.11.0.88", "http://youporn.com/pirata.py", LocalDateTime.parse("2020-09-02T10:00:00"))
      val respuesta8 = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/playa.exe", LocalDateTime.now())
      respuesta8.codigo.shouldBe(CodigoHttp.NOT_FOUND)
      respuesta8.body.shouldBe("")
      respuesta8.tiempo.shouldBe(10)
      val respuesta9 = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/playa.exe", LocalDateTime.now())
      val respuesta10 = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/playa.exe", LocalDateTime.now())
      servidor.enviarALosAnalizadores(respuesta1)
      servidor.enviarALosAnalizadores(respuesta2)
      servidor.enviarALosAnalizadores(respuesta3)
      servidor.enviarALosAnalizadores(respuesta4)
      servidor.enviarALosAnalizadores(respuesta5)
      servidor.enviarALosAnalizadores(respuesta6)
      servidor.enviarALosAnalizadores(respuesta7)
      servidor.enviarALosAnalizadores(respuesta8)
      servidor.enviarALosAnalizadores(respuesta9)
      servidor.enviarALosAnalizadores(respuesta10)
      // porcentaje de pedidos con respuesta exitosa
      analizadorEstadisticas.porcentajePedidosExistosos(servidor).shouldBe(70)
    }

    it("porcentaje de pedidos con respuesta exitosa, con varios analizadores: Analizador de estadisticas") {
      // modulos que ya estan cargados:
      // Modulo(listOf("txt"), "todo bien", 100)
      // Modulo(listOf("jpg", "gif"), "qué linda foto", 100)
      // Modulo(listOf("docx", "odt"), "documento a la vista", 100)
      // definiendo modulos adicionales
      val moduloPY = Modulo(listOf("py", "ipynb"), "Archivos python", 120)
      servidor.agregarModulo(moduloPY)
      val moduloASP = Modulo(listOf("asp", "html"), "Archivos web", 120)
      servidor.agregarModulo(moduloASP)
      // agregando analizador de estadistica
      servidor.agregarAnalizador(analizadorEstadisticas)
      // agregando varios analizadores
      servidor.agregarAnalizador(analizadorDemora)
      servidor.agregarAnalizador(analizadorIPs)
      val analizadorEstadisticas2 = AnalizadorEstadistica()
      servidor.agregarAnalizador(analizadorEstadisticas2)
      // enviando mensajes a los analizadores
      val respuesta3 = servidor.realizarPedido("200.51.101.1", "http://youporn.com/secreto.docx", LocalDateTime.parse("2020-08-30T09:55:00"))
      val respuesta4 = servidor.realizarPedido("200.51.101.1", "http://youporn.com/Password.py", LocalDateTime.parse("2020-08-31T09:00:00"))
      val respuesta5 = servidor.realizarPedido("201.11.0.88", "http://youporn.com/pirata.py", LocalDateTime.parse("2020-08-31T10:00:00"))
      val respuesta6 = servidor.realizarPedido("201.11.0.88", "http://youporn.com/pirata.docx", LocalDateTime.parse("2020-09-01T10:00:00"))
      val respuesta7 = servidor.realizarPedido("201.11.0.88", "http://youporn.com/pirata.py", LocalDateTime.parse("2020-09-02T10:00:00"))
      val respuesta8 = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/playa.exe", LocalDateTime.now())
      respuesta8.codigo.shouldBe(CodigoHttp.NOT_FOUND)
      respuesta8.body.shouldBe("")
      respuesta8.tiempo.shouldBe(10)
      val respuesta9 = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/playa.exe", LocalDateTime.now())
      val respuesta10 = servidor.realizarPedido("207.46.13.5", "http://pepito.com.ar/playa.exe", LocalDateTime.now())
      servidor.enviarALosAnalizadores(respuesta1)
      servidor.enviarALosAnalizadores(respuesta2)
      servidor.enviarALosAnalizadores(respuesta3)
      servidor.enviarALosAnalizadores(respuesta4)
      servidor.enviarALosAnalizadores(respuesta5)
      servidor.enviarALosAnalizadores(respuesta6)
      servidor.enviarALosAnalizadores(respuesta7)
      servidor.enviarALosAnalizadores(respuesta8)
      servidor.enviarALosAnalizadores(respuesta9)
      servidor.enviarALosAnalizadores(respuesta10)
      // porcentaje de pedidos con respuesta exitosa
      analizadorEstadisticas2.porcentajePedidosExistosos(servidor).shouldBe(70)
    }

  }
})
