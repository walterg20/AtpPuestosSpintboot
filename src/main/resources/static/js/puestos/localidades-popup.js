/*
 * Popup de seleccion manual de localidades (modulo 10) - JS puntual.
 *
 * Vanilla, sin framework, sin bundler ni recursos externos. Abre /localidades en una ventana
 * nueva y, al seleccionar una fila, asigna los 3 valores al formulario padre mediante
 * window.opener.setLocalidad(locCod, locNom, provNombre) y cierra el popup.
 */
(function () {
  'use strict';

  var URL_POPUP = '/localidades';
  var NOMBRE_VENTANA = 'localidadesPopup';
  var OPCIONES_VENTANA = 'width=800,height=600';

  var CAMPOS = {
    ven: { cod: 'f276VenCLo', nom: 'f276VenLoc', prov: 'f276VenProv' },
    des: { cod: 'f276DestCL', nom: 'f276DestLoc', prov: 'f276DestProv' },
    tran: { cod: 'f276TranCL', nom: 'f276TranLoc', prov: 'f276TranProv' }
  };

  // Alias historicos (compatibilidad con posibles selectores del formulario padre).
  var ALIAS = {
    ven: { cod: 'locCodVen', nom: 'locNomVen', prov: 'provVenNom' },
    des: { cod: 'locCodDes', nom: 'locNomDes', prov: 'provDesNom' },
    tran: { cod: 'locCodTran', nom: 'locNomTran', prov: 'provTranNom' }
  };

  function setValor(doc, id, valor) {
    if (!doc) {
      return;
    }
    var el = doc.getElementById(id);
    if (el) {
      el.value = (valor == null ? '' : valor);
    }
  }

  function setValorSelector(doc, selector, valor) {
    if (!doc) {
      return;
    }
    var el = doc.querySelector(selector);
    if (el) {
      el.value = (valor == null ? '' : valor);
    }
  }

  window.setLocalidad = function (locCod, locNom, provNombre) {
    var doc = (window.opener && window.opener.document) ? window.opener.document : null;
    if (doc) {
      var target = window.opener.__localidadTarget || 'ven';
      var camposTarget = CAMPOS[target] || CAMPOS.ven;
      setValor(doc, camposTarget.cod, locCod);
      setValor(doc, camposTarget.nom, locNom);
      setValor(doc, camposTarget.prov, provNombre);

      var aliasTarget = ALIAS[target] || ALIAS.ven;
      setValor(doc, aliasTarget.cod, locCod);
      setValor(doc, aliasTarget.nom, locNom);
      setValor(doc, aliasTarget.prov, provNombre);

      // Selectores alternativos por si el formulario usa otros ids.
      setValorSelector(doc, '#locCodVen', locCod);
      setValorSelector(doc, '#locNomVen', locNom);
      setValorSelector(doc, '#provVenNom', provNombre);
    }
    window.close();
  };

  function abrirPopup(target) {
    if (window.opener) {
      window.opener.__localidadTarget = target || 'ven';
    }
    window.open(URL_POPUP, NOMBRE_VENTANA, OPCIONES_VENTANA);
  }

  function activar() {
    var disparadores = document.querySelectorAll('.localidad-popup-trigger[data-popup-enabled="true"]');
    Array.prototype.forEach.call(disparadores, function (btn) {
      btn.addEventListener('click', function (evento) {
        evento.preventDefault();
        abrirPopup(btn.getAttribute('data-target'));
      });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', activar);
  } else {
    activar();
  }
})();
