! function() {
	"use strict";
	var n = "undefined" != typeof globalThis ? globalThis : "undefined" != typeof window ? window : "undefined" != typeof global ? global : "undefined" != typeof self ? self : {};

	function e(e, t) {
		return e(t = {
			exports: {}
		}, t.exports), t.exports
	}
	var t = e(function(e, t) {
			var n;
			(n = t).nodeListForEach = function o(e, t) {
				if (window.NodeList.prototype.forEach) return e.forEach(t);
				for (var n = 0; n < e.length; n++) t.call(window, e[n], n, e)
			}, n.generateUniqueID = function r() {
				var n = (new Date).getTime();
				return "undefined" != typeof window.performance && "function" == typeof window.performance.now && (n += window.performance.now()), "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function(e) {
					var t = (n + 16 * Math.random()) % 16 | 0;
					return n = Math.floor(n / 16), ("x" === e ? t : 3 & t | 8).toString(16)
				})
			}
		}),
		o = e(function(e, t) {
			e.exports = function() {
				function o(e, t) {
					if (window.NodeList.prototype.forEach) return e.forEach(t);
					for (var n = 0; n < e.length; n++) t.call(window, e[n], n, e)
				}

				function e(e) {
					this.$module = e,
					 this.moduleId = e.getAttribute("id"),
					  this.$sections = e.querySelectorAll(".govuk-accordion__section"),
					   this.$openAllButton = "",
					    this.browserSupportsSessionStorage = t.checkForSessionStorage(),
					     this.controlsClass = "govuk-accordion__controls",
					      this.openAllClass = "govuk-accordion__open-all",
					       this.iconClass = "govuk-accordion__icon",
					        this.sectionHeaderClass = "govuk-accordion__section-header",
					         this.sectionHeaderFocusedClass = "govuk-accordion__section-header--focused",
					          this.sectionHeadingClass = "govuk-accordion__section-heading",
					           this.sectionSummaryClass = "govuk-accordion__section-summary",
					            this.sectionButtonClass = "govuk-accordion__section-button",
					             this.sectionExpandedClass = "govuk-accordion__section--expanded"
				}(function(e) {
					var t = "defineProperty" in Object && function() {
						try {
							return Object.defineProperty({}, "test", {
								value: 42
							}), !0
						} catch (e) {
							return !1
						}
					}();
					t || function(a) {
						var s = Object.prototype.hasOwnProperty("__defineGetter__"),
							l = "Getters & setters cannot be defined on this javascript engine",
							u = "A property cannot both have accessors and be writable or have a value";
						Object.defineProperty = function(e, t, n) {
							if (a && (e === window || e === document || e === Element.prototype || e instanceof Element)) return a(e, t, n);
							if (null === e || !(e instanceof Object || "object" == typeof e)) throw new TypeError("Object.defineProperty called on non-object");
							if (!(n instanceof Object)) throw new TypeError("Property description must be an object");
							var o = String(t),
								r = "value" in n || "writable" in n,
								i = "get" in n && typeof n.get,
								c = "set" in n && typeof n.set;
							if (i) {
								if ("function" !== i) throw new TypeError("Getter must be a function");
								if (!s) throw new TypeError(l);
								if (r) throw new TypeError(u);
								Object.__defineGetter__.call(e, o, n.get)
							} else e[o] = n.value;
							if (c) {
								if ("function" !== c) throw new TypeError("Setter must be a function");
								if (!s) throw new TypeError(l);
								if (r) throw new TypeError(u);
								Object.__defineSetter__.call(e, o, n.set)
							}
							return "value" in n && (e[o] = n.value), e
						}
					}(Object.defineProperty)
				}).call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						var t = "bind" in Function.prototype;
						t || Object.defineProperty(Function.prototype, "bind", {
							value: function(t) {
								var n, e = Array,
									o = Object,
									r = o.prototype,
									i = e.prototype,
									c = function c() {},
									a = r.toString,
									s = "function" == typeof Symbol && "symbol" == typeof Symbol.toStringTag,
									l = Function.prototype.toString,
									u = function u(e) {
										try {
											return l.call(e), !0
										} catch (t) {
											return !1
										}
									};
								n = function n(e) {
									if ("function" != typeof e) return !1;
									if (s) return u(e);
									var t = a.call(e);
									return "[object Function]" === t || "[object GeneratorFunction]" === t
								};
								var p = i.slice,
									d = i.concat,
									f = i.push,
									m = Math.max,
									y = this;
								if (!n(y)) throw new TypeError("Function.prototype.bind called on incompatible " + y);
								for (var h, b = p.call(arguments, 1), v = m(0, y.length - b.length), w = [], E = 0; E < v; E++) f.call(w, "$" + E);
								return h = Function("binder", "return function (" + w.join(",") + "){ return binder.apply(this, arguments); }")(function() {
									if (this instanceof h) {
										var e = y.apply(this, d.call(b, p.call(arguments)));
										return o(e) === e ? e : this
									}
									return y.apply(t, d.call(b, p.call(arguments)))
								}), y.prototype && (c.prototype = y.prototype, h.prototype = new c, c.prototype = null), h
							}
						})
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(o) {
						var e = "DOMTokenList" in this && function(e) {
							return !("classList" in e) || !e.classList.toggle("x", !1) && !e.className
						}(document.createElement("x"));
						e || function(e) {
							var t = "DOMTokenList" in e && e.DOMTokenList;
							t && (!document.createElementNS || !document.createElementNS("http://www.w3.org/2000/svg", "svg") || document.createElementNS("http://www.w3.org/2000/svg", "svg").classList instanceof DOMTokenList) || (e.DOMTokenList = function() {
								var r = !0,
									n = function(e, t, n, o) {
										Object.defineProperty ? Object.defineProperty(e, t, {
											configurable: !1 === r || !!o,
											get: n
										}) : e.__defineGetter__(t, n)
									};
								try {
									n({}, "support")
								} catch (e) {
									r = !1
								}
								return function(r, i) {
									var c = this,
										a = [],
										s = {},
										l = 0,
										e = 0,
										t = function(e) {
											n(c, e, function() {
												return p(), a[e]
											}, !1)
										},
										u = function() {
											if (e <= l)
												for (; e < l; ++e) t(e)
										},
										p = function() {
											var e, t, n = arguments,
												o = /\s+/;
											if (n.length)
												for (t = 0; t < n.length; ++t)
													if (o.test(n[t])) throw (e = new SyntaxError('String "' + n[t] + '" contains an invalid character')).code = 5, e.name = "InvalidCharacterError", e;
											for ("" === (a = "object" == typeof r[i] ? ("" + r[i].baseVal).replace(/^\s+|\s+$/g, "").split(o) : ("" + r[i]).replace(/^\s+|\s+$/g, "").split(o))[0] && (a = []), s = {}, t = 0; t < a.length; ++t) s[a[t]] = !0;
											l = a.length, u()
										};
									return p(), n(c, "length", function() {
										return p(), l
									}), c.toLocaleString = c.toString = function() {
										return p(), a.join(" ")
									}, c.item = function(e) {
										return p(), a[e]
									}, c.contains = function(e) {
										return p(), !!s[e]
									}, c.add = function() {
										p.apply(c, e = arguments);
										for (var e, t, n = 0, o = e.length; n < o; ++n) t = e[n], s[t] || (a.push(t), s[t] = !0);
										l !== a.length && (l = a.length >>> 0, "object" == typeof r[i] ? r[i].baseVal = a.join(" ") : r[i] = a.join(" "), u())
									}, c.remove = function() {
										p.apply(c, e = arguments);
										for (var e, t = {}, n = 0, o = []; n < e.length; ++n) t[e[n]] = !0, delete s[e[n]];
										for (n = 0; n < a.length; ++n) t[a[n]] || o.push(a[n]);
										l = (a = o).length >>> 0, "object" == typeof r[i] ? r[i].baseVal = a.join(" ") : r[i] = a.join(" "), u()
									}, c.toggle = function(e, t) {
										return p.apply(c, [e]), o !== t ? t ? (c.add(e), !0) : (c.remove(e), !1) : s[e] ? (c.remove(e), !1) : (c.add(e), !0)
									}, c
								}
							}()),
								function() {
									var e = document.createElement("span");
									"classList" in e && (e.classList.toggle("x", !1), e.classList.contains("x") && (e.classList.constructor.prototype.toggle = function(e) {
										var t = arguments[1];
										if (t !== o) return this[(t = !!t) ? "add" : "remove"](e), t;
										var n = !this.contains(e);
										return this[n ? "add" : "remove"](e), n
									}))
								}(),
								function() {
									var e = document.createElement("span");
									if ("classList" in e && (e.classList.add("a", "b"), !e.classList.contains("b"))) {
										var o = e.classList.constructor.prototype.add;
										e.classList.constructor.prototype.add = function() {
											for (var e = arguments, t = arguments.length, n = 0; n < t; n++) o.call(this, e[n])
										}
									}
								}(),
								function() {
									var e = document.createElement("span");
									if ("classList" in e && (e.classList.add("a"), e.classList.add("b"), e.classList.remove("a", "b"), e.classList.contains("b"))) {
										var o = e.classList.constructor.prototype.remove;
										e.classList.constructor.prototype.remove = function() {
											for (var e = arguments, t = arguments.length, n = 0; n < t; n++) o.call(this, e[n])
										}
									}
								}()
						}(this)
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						var t = "Document" in this;
						t || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && (this.HTMLDocument ? this.Document = this.HTMLDocument : (this.Document = this.HTMLDocument = document.constructor = new Function("return function Document() {}")(), this.Document.prototype = document))
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						var t = "Element" in this && "HTMLElement" in this;
						t || function() {
							if (!window.Element || window.HTMLElement) {
								window.Element = window.HTMLElement = new Function("return function Element() {}")();
								var e, t = document.appendChild(document.createElement("body")),
									n = t.appendChild(document.createElement("iframe")),
									o = n.contentWindow.document,
									a = Element.prototype = o.appendChild(o.createElement("*")),
									s = {},
									l = function(e, t) {
										var n, o, r, i = e.childNodes || [],
											c = -1;
										if (1 === e.nodeType && e.constructor !== Element)
											for (n in e.constructor = Element, s) o = s[n], e[n] = o;
										for (; r = t && i[++c];) l(r, t);
										return e
									},
									u = document.getElementsByTagName("*"),
									r = document.createElement,
									i = 100;
								a.attachEvent("onpropertychange", function(e) {
									for (var t, n = e.propertyName, o = !s.hasOwnProperty(n), r = a[n], i = s[n], c = -1; t = u[++c];) 1 === t.nodeType && (o || t[n] === i) && (t[n] = r);
									s[n] = r
								}), a.constructor = Element, a.hasAttribute || (a.hasAttribute = function(e) {
									return null !== this.getAttribute(e)
								}), c() || (document.onreadystatechange = c, e = setInterval(c, 25)), document.createElement = function(e) {
									var t = r(String(e).toLowerCase());
									return l(t)
								}, document.removeChild(t)
							} else window.HTMLElement = window.Element;

							function c() {
								return i-- || clearTimeout(e), !(!document.body || document.body.prototype || !/(complete|interactive)/.test(document.readyState) || (l(document, !0), e && document.body.prototype && clearTimeout(e), !document.body.prototype))
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						var t = "document" in this && "classList" in document.documentElement && "Element" in this && "classList" in Element.prototype && function() {
							var e = document.createElement("span");
							return e.classList.add("a", "b"), e.classList.contains("b")
						}();
						t || function(e) {
							var u = !0,
								p = function(e, t, n, o) {
									Object.defineProperty ? Object.defineProperty(e, t, {
										configurable: !1 === u || !!o,
										get: n
									}) : e.__defineGetter__(t, n)
								};
							try {
								p({}, "support")
							} catch (t) {
								u = !1
							}
							var d = function(e, s, l) {
								p(e.prototype, s, function() {
									var e, t = this,
										n = "__defineGetter__DEFINE_PROPERTY" + s;
									if (t[n]) return e;
									if (!(t[n] = !0) === u) {
										for (var o, r = d.mirror || document.createElement("div"), i = r.childNodes, c = i.length, a = 0; a < c; ++a)
											if (i[a]._R === t) {
												o = i[a];
												break
											} o || (o = r.appendChild(document.createElement("div"))), e = DOMTokenList.call(o, t, l)
									} else e = new DOMTokenList(t, l);
									return p(t, s, function() {
										return e
									}), delete t[n], e
								}, !0)
							};
							d(e.Element, "classList", "className"), d(e.HTMLElement, "classList", "className"), d(e.HTMLLinkElement, "relList", "rel"), d(e.HTMLAnchorElement, "relList", "rel"), d(e.HTMLAreaElement, "relList", "rel")
						}(this)
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}), e.prototype.init = function() {
					if (this.$module) {
						this.initControls(), this.initSectionHeaders();
						var e = this.checkIfAllSectionsOpen();
						this.updateOpenAllButton(e)
					}
				}, e.prototype.initControls = function() {
					this.$openAllButton = document.createElement("button"), this.$openAllButton.setAttribute("type", "button"), this.$openAllButton.innerHTML = 'Open all <span class="govuk-visually-hidden">sections</span>', this.$openAllButton.setAttribute("class", this.openAllClass), this.$openAllButton.setAttribute("aria-expanded", "false"), this.$openAllButton.setAttribute("type", "button");
					var e = document.createElement("div");
					e.setAttribute("class", this.controlsClass), e.appendChild(this.$openAllButton), this.$module.insertBefore(e, this.$module.firstChild), this.$openAllButton.addEventListener("click", this.onOpenOrCloseAllToggle.bind(this))
				}, e.prototype.initSectionHeaders = function() {
					o(this.$sections, function(e, t) {
						var n = e.querySelector("." + this.sectionHeaderClass);
						this.initHeaderAttributes(n, t), this.setExpanded(this.isExpanded(e), e), n.addEventListener("click", this.onSectionToggle.bind(this, e)), this.setInitialState(e)
					}.bind(this))
				}, e.prototype.initHeaderAttributes = function(t, e) {
					var n = this,
						o = t.querySelector("." + this.sectionButtonClass),
						r = t.querySelector("." + this.sectionHeadingClass),
						i = t.querySelector("." + this.sectionSummaryClass),
						c = document.createElement("button");
					c.setAttribute("type", "button"), c.setAttribute("id", this.moduleId + "-heading-" + (e + 1)), c.setAttribute("aria-controls", this.moduleId + "-content-" + (e + 1));
					for (var a = 0; a < o.attributes.length; a++) {
						var s = o.attributes.item(a);
						c.setAttribute(s.nodeName, s.nodeValue)
					}
					c.addEventListener("focusin", function(e) {
						t.classList.contains(n.sectionHeaderFocusedClass) || (t.className += " " + n.sectionHeaderFocusedClass)
					}), c.addEventListener("blur", function(e) {
						t.classList.remove(n.sectionHeaderFocusedClass)
					}), null != i && c.setAttribute("aria-describedby", this.moduleId + "-summary-" + (e + 1)), c.innerHTML = o.innerHTML, r.removeChild(o), r.appendChild(c);
					var l = document.createElement("span");
					l.className = this.iconClass, l.setAttribute("aria-hidden", "true"), r.appendChild(l)
				}, e.prototype.onSectionToggle = function(e) {
					var t = this.isExpanded(e);
					this.setExpanded(!t, e), this.storeState(e)
				}, e.prototype.onOpenOrCloseAllToggle = function() {
					var t = this,
						e = this.$sections,
						n = !this.checkIfAllSectionsOpen();
					o(e, function(e) {
						t.setExpanded(n, e), t.storeState(e)
					}), t.updateOpenAllButton(n)
				}, e.prototype.setExpanded = function(e, t) {
					var n = t.querySelector("." + this.sectionButtonClass);
					n.setAttribute("aria-expanded", e), e ? t.classList.add(this.sectionExpandedClass) : t.classList.remove(this.sectionExpandedClass);
					var o = this.checkIfAllSectionsOpen();
					this.updateOpenAllButton(o)
				}, e.prototype.isExpanded = function(e) {
					return e.classList.contains(this.sectionExpandedClass)
				}, e.prototype.checkIfAllSectionsOpen = function() {
					var e = this.$sections.length,
						t = this.$module.querySelectorAll("." + this.sectionExpandedClass).length,
						n = e === t;
					return n
				}, e.prototype.updateOpenAllButton = function(e) {
					var t = e ? accordianMessages.accordianCloseMessage : accordianMessages.accordianOpenMessage;
					t += '<span class="govuk-visually-hidden"> sections</span>', this.$openAllButton.setAttribute("aria-expanded", e), this.$openAllButton.innerHTML = t
				};
				var t = {
					checkForSessionStorage: function() {
						var e, t = "this is the test string";
						try {
							return window.sessionStorage.setItem(t, t), e = window.sessionStorage.getItem(t) === t.toString(), window.sessionStorage.removeItem(t), e
						} catch (n) {
							"undefined" != typeof console && "undefined" != typeof console.log || console.log("Notice: sessionStorage not available.")
						}
					}
				};
				return e.prototype.storeState = function(e) {
					if (this.browserSupportsSessionStorage) {
						var t = e.querySelector("." + this.sectionButtonClass);
						if (t) {
							var n = t.getAttribute("aria-controls"),
								o = t.getAttribute("aria-expanded");
							void 0 !== n || "undefined" != typeof console && "undefined" != typeof console.log || console.error(new Error("No aria controls present in accordion section heading.")), void 0 !== o || "undefined" != typeof console && "undefined" != typeof console.log || console.error(new Error("No aria expanded present in accordion section heading.")), n && o && window.sessionStorage.setItem(n, o)
						}
					}
				}, e.prototype.setInitialState = function(e) {
					if (this.browserSupportsSessionStorage) {
						var t = e.querySelector("." + this.sectionButtonClass);
						if (t) {
							var n = t.getAttribute("aria-controls"),
								o = n ? window.sessionStorage.getItem(n) : null;
							null !== o && this.setExpanded("true" === o, e)
						}
					}
				}, e
			}()
		}),
		r = e(function(e, t) {
			e.exports = function() {
				function e(e) {
					this.$module = e, this.debounceFormSubmitTimer = null
				}
				return function(e) {
					var t = "Window" in this;
					t || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && function(e) {
						e.constructor ? e.Window = e.constructor : (e.Window = e.constructor = new Function("return function Window() {}")()).prototype = this
					}(this)
				}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						var t = "Document" in this;
						t || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && (this.HTMLDocument ? this.Document = this.HTMLDocument : (this.Document = this.HTMLDocument = document.constructor = new Function("return function Document() {}")(), this.Document.prototype = document))
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						var t = "Element" in this && "HTMLElement" in this;
						t || function() {
							if (!window.Element || window.HTMLElement) {
								window.Element = window.HTMLElement = new Function("return function Element() {}")();
								var e, t = document.appendChild(document.createElement("body")),
									n = t.appendChild(document.createElement("iframe")),
									o = n.contentWindow.document,
									a = Element.prototype = o.appendChild(o.createElement("*")),
									s = {},
									l = function(e, t) {
										var n, o, r, i = e.childNodes || [],
											c = -1;
										if (1 === e.nodeType && e.constructor !== Element)
											for (n in e.constructor = Element, s) o = s[n], e[n] = o;
										for (; r = t && i[++c];) l(r, t);
										return e
									},
									u = document.getElementsByTagName("*"),
									r = document.createElement,
									i = 100;
								a.attachEvent("onpropertychange", function(e) {
									for (var t, n = e.propertyName, o = !s.hasOwnProperty(n), r = a[n], i = s[n], c = -1; t = u[++c];) 1 === t.nodeType && (o || t[n] === i) && (t[n] = r);
									s[n] = r
								}), a.constructor = Element, a.hasAttribute || (a.hasAttribute = function(e) {
									return null !== this.getAttribute(e)
								}), c() || (document.onreadystatechange = c, e = setInterval(c, 25)), document.createElement = function(e) {
									var t = r(String(e).toLowerCase());
									return l(t)
								}, document.removeChild(t)
							} else window.HTMLElement = window.Element;

							function c() {
								return i-- || clearTimeout(e), !(!document.body || document.body.prototype || !/(complete|interactive)/.test(document.readyState) || (l(document, !0), e && document.body.prototype && clearTimeout(e), !document.body.prototype))
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						var t = "defineProperty" in Object && function() {
							try {
								return Object.defineProperty({}, "test", {
									value: 42
								}), !0
							} catch (e) {
								return !1
							}
						}();
						t || function(a) {
							var s = Object.prototype.hasOwnProperty("__defineGetter__"),
								l = "Getters & setters cannot be defined on this javascript engine",
								u = "A property cannot both have accessors and be writable or have a value";
							Object.defineProperty = function(e, t, n) {
								if (a && (e === window || e === document || e === Element.prototype || e instanceof Element)) return a(e, t, n);
								if (null === e || !(e instanceof Object || "object" == typeof e)) throw new TypeError("Object.defineProperty called on non-object");
								if (!(n instanceof Object)) throw new TypeError("Property description must be an object");
								var o = String(t),
									r = "value" in n || "writable" in n,
									i = "get" in n && typeof n.get,
									c = "set" in n && typeof n.set;
								if (i) {
									if ("function" !== i) throw new TypeError("Getter must be a function");
									if (!s) throw new TypeError(l);
									if (r) throw new TypeError(u);
									Object.__defineGetter__.call(e, o, n.get)
								} else e[o] = n.value;
								if (c) {
									if ("function" !== c) throw new TypeError("Setter must be a function");
									if (!s) throw new TypeError(l);
									if (r) throw new TypeError(u);
									Object.__defineSetter__.call(e, o, n.set)
								}
								return "value" in n && (e[o] = n.value), e
							}
						}(Object.defineProperty)
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(i) {
						var e = function(e) {
							if (!("Event" in e)) return !1;
							if ("function" == typeof e.Event) return !0;
							try {
								return new Event("click"), !0
							} catch (t) {
								return !1
							}
						}(this);
						e || function() {
							var n = {
								click: 1,
								dblclick: 1,
								keyup: 1,
								keypress: 1,
								keydown: 1,
								mousedown: 1,
								mouseup: 1,
								mousemove: 1,
								mouseover: 1,
								mouseenter: 1,
								mouseleave: 1,
								mouseout: 1,
								storage: 1,
								storagecommit: 1,
								textinput: 1
							};
							if ("undefined" != typeof document && "undefined" != typeof window) {
								var e = window.Event && window.Event.prototype || null;
								window.Event = Window.prototype.Event = function(e, t) {
									if (!e) throw new Error("Not enough arguments");
									var n;
									if ("createEvent" in document) {
										n = document.createEvent("Event");
										var o = !(!t || t.bubbles === i) && t.bubbles,
											r = !(!t || t.cancelable === i) && t.cancelable;
										return n.initEvent(e, o, r), n
									}
									return (n = document.createEventObject()).type = e, n.bubbles = !(!t || t.bubbles === i) && t.bubbles, n.cancelable = !(!t || t.cancelable === i) && t.cancelable, n
								}, e && Object.defineProperty(window.Event, "prototype", {
									configurable: !1,
									enumerable: !1,
									writable: !0,
									value: e
								}), "createEvent" in document || (window.addEventListener = Window.prototype.addEventListener = Document.prototype.addEventListener = Element.prototype.addEventListener = function() {
									var c = this,
										e = arguments[0],
										t = arguments[1];
									if (c === window && e in n) throw new Error("In IE8 the event: " + e + " is not available on the window object. Please see https://github.com/Financial-Times/polyfill-service/issues/317 for more information.");
									c._events || (c._events = {}), c._events[e] || (c._events[e] = function(e) {
										var t, n = c._events[e.type].list,
											o = n.slice(),
											r = -1,
											i = o.length;
										for (e.preventDefault = function() {
											!1 !== e.cancelable && (e.returnValue = !1)
										}, e.stopPropagation = function() {
											e.cancelBubble = !0
										}, e.stopImmediatePropagation = function() {
											e.cancelBubble = !0, e.cancelImmediate = !0
										}, e.currentTarget = c, e.relatedTarget = e.fromElement || null, e.target = e.target || e.srcElement || c, e.timeStamp = (new Date).getTime(), e.clientX && (e.pageX = e.clientX + document.documentElement.scrollLeft, e.pageY = e.clientY + document.documentElement.scrollTop); ++r < i && !e.cancelImmediate;) r in o && (t = o[r], -1 !== a(n, t) && "function" == typeof t && t.call(c, e))
									}, c._events[e].list = [], c.attachEvent && c.attachEvent("on" + e, c._events[e])), c._events[e].list.push(t)
								}, window.removeEventListener = Window.prototype.removeEventListener = Document.prototype.removeEventListener = Element.prototype.removeEventListener = function() {
									var e, t = this,
										n = arguments[0],
										o = arguments[1];
									t._events && t._events[n] && t._events[n].list && -1 !== (e = a(t._events[n].list, o)) && (t._events[n].list.splice(e, 1), t._events[n].list.length || (t.detachEvent && t.detachEvent("on" + n, t._events[n]), delete t._events[n]))
								}, window.dispatchEvent = Window.prototype.dispatchEvent = Document.prototype.dispatchEvent = Element.prototype.dispatchEvent = function(e) {
									if (!arguments.length) throw new Error("Not enough arguments");
									if (!e || "string" != typeof e.type) throw new Error("DOM Events Exception 0");
									var t = this,
										n = e.type;
									try {
										if (!e.bubbles) {
											e.cancelBubble = !0;
											var o = function(e) {
												e.cancelBubble = !0, (t || window).detachEvent("on" + n, o)
											};
											this.attachEvent("on" + n, o)
										}
										this.fireEvent("on" + n, e)
									} catch (r) {
										for (e.target = t;
											 "_events" in (e.currentTarget = t) && "function" == typeof t._events[n] && t._events[n].call(t, e), "function" == typeof t["on" + n] && t["on" + n].call(t, e), (t = 9 === t.nodeType ? t.parentWindow : t.parentNode) && !e.cancelBubble;);
									}
									return !0
								}, document.attachEvent("onreadystatechange", function() {
									"complete" === document.readyState && document.dispatchEvent(new Event("DOMContentLoaded", {
										bubbles: !0
									}))
								}))
							}

							function a(e, t) {
								for (var n = -1, o = e.length; ++n < o;)
									if (n in e && e[n] === t) return n;
								return -1
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						var t = "bind" in Function.prototype;
						t || Object.defineProperty(Function.prototype, "bind", {
							value: function(t) {
								var n, e = Array,
									o = Object,
									r = o.prototype,
									i = e.prototype,
									c = function c() {},
									a = r.toString,
									s = "function" == typeof Symbol && "symbol" == typeof Symbol.toStringTag,
									l = Function.prototype.toString,
									u = function u(e) {
										try {
											return l.call(e), !0
										} catch (t) {
											return !1
										}
									};
								n = function n(e) {
									if ("function" != typeof e) return !1;
									if (s) return u(e);
									var t = a.call(e);
									return "[object Function]" === t || "[object GeneratorFunction]" === t
								};
								var p = i.slice,
									d = i.concat,
									f = i.push,
									m = Math.max,
									y = this;
								if (!n(y)) throw new TypeError("Function.prototype.bind called on incompatible " + y);
								for (var h, b = p.call(arguments, 1), v = m(0, y.length - b.length), w = [], E = 0; E < v; E++) f.call(w, "$" + E);
								return h = Function("binder", "return function (" + w.join(",") + "){ return binder.apply(this, arguments); }")(function() {
									if (this instanceof h) {
										var e = y.apply(this, d.call(b, p.call(arguments)));
										return o(e) === e ? e : this
									}
									return y.apply(t, d.call(b, p.call(arguments)))
								}), y.prototype && (c.prototype = y.prototype, h.prototype = new c, c.prototype = null), h
							}
						})
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}), e.prototype.handleKeyDown = function(e) {
					var t = e.target;
					"button" === t.getAttribute("role") && 32 === e.keyCode && (e.preventDefault(), t.click())
				}, e.prototype.debounce = function(e) {
					if ("true" === e.target.getAttribute("data-prevent-double-click")) return this.debounceFormSubmitTimer ? (e.preventDefault(), !1) : void(this.debounceFormSubmitTimer = setTimeout(function() {
						this.debounceFormSubmitTimer = null
					}.bind(this), 1e3))
				}, e.prototype.init = function() {
					this.$module.addEventListener("keydown", this.handleKeyDown), this.$module.addEventListener("click", this.debounce)
				}, e
			}()
		}),
		i = e(function(e, t) {
			e.exports = function() {
				function e(e) {
					this.$module = e
				}
				return function(e) {
					var t = "defineProperty" in Object && function() {
						try {
							return Object.defineProperty({}, "test", {
								value: 42
							}), !0
						} catch (e) {
							return !1
						}
					}();
					t || function(a) {
						var s = Object.prototype.hasOwnProperty("__defineGetter__"),
							l = "Getters & setters cannot be defined on this javascript engine",
							u = "A property cannot both have accessors and be writable or have a value";
						Object.defineProperty = function(e, t, n) {
							if (a && (e === window || e === document || e === Element.prototype || e instanceof Element)) return a(e, t, n);
							if (null === e || !(e instanceof Object || "object" == typeof e)) throw new TypeError("Object.defineProperty called on non-object");
							if (!(n instanceof Object)) throw new TypeError("Property description must be an object");
							var o = String(t),
								r = "value" in n || "writable" in n,
								i = "get" in n && typeof n.get,
								c = "set" in n && typeof n.set;
							if (i) {
								if ("function" !== i) throw new TypeError("Getter must be a function");
								if (!s) throw new TypeError(l);
								if (r) throw new TypeError(u);
								Object.__defineGetter__.call(e, o, n.get)
							} else e[o] = n.value;
							if (c) {
								if ("function" !== c) throw new TypeError("Setter must be a function");
								if (!s) throw new TypeError(l);
								if (r) throw new TypeError(u);
								Object.__defineSetter__.call(e, o, n.set)
							}
							return "value" in n && (e[o] = n.value), e
						}
					}(Object.defineProperty)
				}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						var t = "bind" in Function.prototype;
						t || Object.defineProperty(Function.prototype, "bind", {
							value: function(t) {
								var n, e = Array,
									o = Object,
									r = o.prototype,
									i = e.prototype,
									c = function c() {},
									a = r.toString,
									s = "function" == typeof Symbol && "symbol" == typeof Symbol.toStringTag,
									l = Function.prototype.toString,
									u = function u(e) {
										try {
											return l.call(e), !0
										} catch (t) {
											return !1
										}
									};
								n = function n(e) {
									if ("function" != typeof e) return !1;
									if (s) return u(e);
									var t = a.call(e);
									return "[object Function]" === t || "[object GeneratorFunction]" === t
								};
								var p = i.slice,
									d = i.concat,
									f = i.push,
									m = Math.max,
									y = this;
								if (!n(y)) throw new TypeError("Function.prototype.bind called on incompatible " + y);
								for (var h, b = p.call(arguments, 1), v = m(0, y.length - b.length), w = [], E = 0; E < v; E++) f.call(w, "$" + E);
								return h = Function("binder", "return function (" + w.join(",") + "){ return binder.apply(this, arguments); }")(function() {
									if (this instanceof h) {
										var e = y.apply(this, d.call(b, p.call(arguments)));
										return o(e) === e ? e : this
									}
									return y.apply(t, d.call(b, p.call(arguments)))
								}), y.prototype && (c.prototype = y.prototype, h.prototype = new c, c.prototype = null), h
							}
						})
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						var t = "Window" in this;
						t || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && function(e) {
							e.constructor ? e.Window = e.constructor : (e.Window = e.constructor = new Function("return function Window() {}")()).prototype = this
						}(this)
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						var t = "Document" in this;
						t || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && (this.HTMLDocument ? this.Document = this.HTMLDocument : (this.Document = this.HTMLDocument = document.constructor = new Function("return function Document() {}")(), this.Document.prototype = document))
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						var t = "Element" in this && "HTMLElement" in this;
						t || function() {
							if (!window.Element || window.HTMLElement) {
								window.Element = window.HTMLElement = new Function("return function Element() {}")();
								var e, t = document.appendChild(document.createElement("body")),
									n = t.appendChild(document.createElement("iframe")),
									o = n.contentWindow.document,
									a = Element.prototype = o.appendChild(o.createElement("*")),
									s = {},
									l = function(e, t) {
										var n, o, r, i = e.childNodes || [],
											c = -1;
										if (1 === e.nodeType && e.constructor !== Element)
											for (n in e.constructor = Element, s) o = s[n], e[n] = o;
										for (; r = t && i[++c];) l(r, t);
										return e
									},
									u = document.getElementsByTagName("*"),
									r = document.createElement,
									i = 100;
								a.attachEvent("onpropertychange", function(e) {
									for (var t, n = e.propertyName, o = !s.hasOwnProperty(n), r = a[n], i = s[n], c = -1; t = u[++c];) 1 === t.nodeType && (o || t[n] === i) && (t[n] = r);
									s[n] = r
								}), a.constructor = Element, a.hasAttribute || (a.hasAttribute = function(e) {
									return null !== this.getAttribute(e)
								}), c() || (document.onreadystatechange = c, e = setInterval(c, 25)), document.createElement = function(e) {
									var t = r(String(e).toLowerCase());
									return l(t)
								}, document.removeChild(t)
							} else window.HTMLElement = window.Element;

							function c() {
								return i-- || clearTimeout(e), !(!document.body || document.body.prototype || !/(complete|interactive)/.test(document.readyState) || (l(document, !0), e && document.body.prototype && clearTimeout(e), !document.body.prototype))
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(i) {
						var e = function(e) {
							if (!("Event" in e)) return !1;
							if ("function" == typeof e.Event) return !0;
							try {
								return new Event("click"), !0
							} catch (t) {
								return !1
							}
						}(this);
						e || function() {
							var n = {
								click: 1,
								dblclick: 1,
								keyup: 1,
								keypress: 1,
								keydown: 1,
								mousedown: 1,
								mouseup: 1,
								mousemove: 1,
								mouseover: 1,
								mouseenter: 1,
								mouseleave: 1,
								mouseout: 1,
								storage: 1,
								storagecommit: 1,
								textinput: 1
							};
							if ("undefined" != typeof document && "undefined" != typeof window) {
								var e = window.Event && window.Event.prototype || null;
								window.Event = Window.prototype.Event = function(e, t) {
									if (!e) throw new Error("Not enough arguments");
									var n;
									if ("createEvent" in document) {
										n = document.createEvent("Event");
										var o = !(!t || t.bubbles === i) && t.bubbles,
											r = !(!t || t.cancelable === i) && t.cancelable;
										return n.initEvent(e, o, r), n
									}
									return (n = document.createEventObject()).type = e, n.bubbles = !(!t || t.bubbles === i) && t.bubbles, n.cancelable = !(!t || t.cancelable === i) && t.cancelable, n
								}, e && Object.defineProperty(window.Event, "prototype", {
									configurable: !1,
									enumerable: !1,
									writable: !0,
									value: e
								}), "createEvent" in document || (window.addEventListener = Window.prototype.addEventListener = Document.prototype.addEventListener = Element.prototype.addEventListener = function() {
									var c = this,
										e = arguments[0],
										t = arguments[1];
									if (c === window && e in n) throw new Error("In IE8 the event: " + e + " is not available on the window object. Please see https://github.com/Financial-Times/polyfill-service/issues/317 for more information.");
									c._events || (c._events = {}), c._events[e] || (c._events[e] = function(e) {
										var t, n = c._events[e.type].list,
											o = n.slice(),
											r = -1,
											i = o.length;
										for (e.preventDefault = function() {
											!1 !== e.cancelable && (e.returnValue = !1)
										}, e.stopPropagation = function() {
											e.cancelBubble = !0
										}, e.stopImmediatePropagation = function() {
											e.cancelBubble = !0, e.cancelImmediate = !0
										}, e.currentTarget = c, e.relatedTarget = e.fromElement || null, e.target = e.target || e.srcElement || c, e.timeStamp = (new Date).getTime(), e.clientX && (e.pageX = e.clientX + document.documentElement.scrollLeft, e.pageY = e.clientY + document.documentElement.scrollTop); ++r < i && !e.cancelImmediate;) r in o && (t = o[r], -1 !== a(n, t) && "function" == typeof t && t.call(c, e))
									}, c._events[e].list = [], c.attachEvent && c.attachEvent("on" + e, c._events[e])), c._events[e].list.push(t)
								}, window.removeEventListener = Window.prototype.removeEventListener = Document.prototype.removeEventListener = Element.prototype.removeEventListener = function() {
									var e, t = this,
										n = arguments[0],
										o = arguments[1];
									t._events && t._events[n] && t._events[n].list && -1 !== (e = a(t._events[n].list, o)) && (t._events[n].list.splice(e, 1), t._events[n].list.length || (t.detachEvent && t.detachEvent("on" + n, t._events[n]), delete t._events[n]))
								}, window.dispatchEvent = Window.prototype.dispatchEvent = Document.prototype.dispatchEvent = Element.prototype.dispatchEvent = function(e) {
									if (!arguments.length) throw new Error("Not enough arguments");
									if (!e || "string" != typeof e.type) throw new Error("DOM Events Exception 0");
									var t = this,
										n = e.type;
									try {
										if (!e.bubbles) {
											e.cancelBubble = !0;
											var o = function(e) {
												e.cancelBubble = !0, (t || window).detachEvent("on" + n, o)
											};
											this.attachEvent("on" + n, o)
										}
										this.fireEvent("on" + n, e)
									} catch (r) {
										for (e.target = t;
											 "_events" in (e.currentTarget = t) && "function" == typeof t._events[n] && t._events[n].call(t, e), "function" == typeof t["on" + n] && t["on" + n].call(t, e), (t = 9 === t.nodeType ? t.parentWindow : t.parentNode) && !e.cancelBubble;);
									}
									return !0
								}, document.attachEvent("onreadystatechange", function() {
									"complete" === document.readyState && document.dispatchEvent(new Event("DOMContentLoaded", {
										bubbles: !0
									}))
								}))
							}

							function a(e, t) {
								for (var n = -1, o = e.length; ++n < o;)
									if (n in e && e[n] === t) return n;
								return -1
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}), e.prototype.init = function() {
					this.$module && ("boolean" == typeof this.$module.open || this.polyfillDetails())
				}, e.prototype.polyfillDetails = function() {
					var e = this.$module,
						t = this.$summary = e.getElementsByTagName("summary").item(0),
						n = this.$content = e.getElementsByTagName("div").item(0);
					t && n && (n.id || (n.id = "details-content-" + function o() {
						var n = (new Date).getTime();
						return "undefined" != typeof window.performance && "function" == typeof window.performance.now && (n += window.performance.now()), "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function(e) {
							var t = (n + 16 * Math.random()) % 16 | 0;
							return n = Math.floor(n / 16), ("x" === e ? t : 3 & t | 8).toString(16)
						})
					}()), e.setAttribute("role", "group"), t.setAttribute("role", "button"), t.setAttribute("aria-controls", n.id), !(t.tabIndex = 0) == (null !== e.getAttribute("open")) ? (t.setAttribute("aria-expanded", "true"), n.setAttribute("aria-hidden", "false")) : (t.setAttribute("aria-expanded", "false"), n.setAttribute("aria-hidden", "true"), n.style.display = "none"), this.polyfillHandleInputs(t, this.polyfillSetAttributes.bind(this)))
				}, e.prototype.polyfillSetAttributes = function() {
					var e = this.$module,
						t = this.$summary,
						n = this.$content,
						o = "true" === t.getAttribute("aria-expanded"),
						r = "true" === n.getAttribute("aria-hidden");
					return t.setAttribute("aria-expanded", o ? "false" : "true"), n.setAttribute("aria-hidden", r ? "false" : "true"), n.style.display = o ? "none" : "", null !== e.getAttribute("open") ? e.removeAttribute("open") : e.setAttribute("open", "open"), !0
				}, e.prototype.polyfillHandleInputs = function(e, n) {
					e.addEventListener("keypress", function(e) {
						var t = e.target;
						13 !== e.keyCode && 32 !== e.keyCode || "summary" === t.nodeName.toLowerCase() && (e.preventDefault(), t.click ? t.click() : n(e))
					}), e.addEventListener("keyup", function(e) {
						var t = e.target;
						32 === e.keyCode && "summary" === t.nodeName.toLowerCase() && e.preventDefault()
					}), e.addEventListener("click", n)
				}, e
			}()
		}),
		c = e(function(e, t) {
			e.exports = function() {
				function e(e) {
					this.$module = e, this.$textarea = e.querySelector(".govuk-js-character-count"), this.$textarea && (this.$countMessage = e.querySelector("[id=" + this.$textarea.id + "-info]"))
				}
				return function(e) {
					var a, s, l, u;
					"defineProperty" in Object && function() {
						try {
							return Object.defineProperty({}, "test", {
								value: 42
							}), !0
						} catch (e) {
							return !1
						}
					}() || (a = Object.defineProperty, s = Object.prototype.hasOwnProperty("__defineGetter__"), l = "Getters & setters cannot be defined on this javascript engine", u = "A property cannot both have accessors and be writable or have a value", Object.defineProperty = function(e, t, n) {
						if (a && (e === window || e === document || e === Element.prototype || e instanceof Element)) return a(e, t, n);
						if (null === e || !(e instanceof Object || "object" == typeof e)) throw new TypeError("Object.defineProperty called on non-object");
						if (!(n instanceof Object)) throw new TypeError("Property description must be an object");
						var o = String(t),
							r = "value" in n || "writable" in n,
							i = "get" in n && typeof n.get,
							c = "set" in n && typeof n.set;
						if (i) {
							if ("function" !== i) throw new TypeError("Getter must be a function");
							if (!s) throw new TypeError(l);
							if (r) throw new TypeError(u);
							Object.__defineGetter__.call(e, o, n.get)
						} else e[o] = n.value;
						if (c) {
							if ("function" !== c) throw new TypeError("Setter must be a function");
							if (!s) throw new TypeError(l);
							if (r) throw new TypeError(u);
							Object.__defineSetter__.call(e, o, n.set)
						}
						return "value" in n && (e[o] = n.value), e
					})
				}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"bind" in Function.prototype || Object.defineProperty(Function.prototype, "bind", {
							value: function(t) {
								var n, e = Array,
									o = Object,
									r = o.prototype,
									i = e.prototype,
									c = function c() {},
									a = r.toString,
									s = "function" == typeof Symbol && "symbol" == typeof Symbol.toStringTag,
									l = Function.prototype.toString,
									u = function u(e) {
										try {
											return l.call(e), !0
										} catch (t) {
											return !1
										}
									};
								n = function n(e) {
									if ("function" != typeof e) return !1;
									if (s) return u(e);
									var t = a.call(e);
									return "[object Function]" === t || "[object GeneratorFunction]" === t
								};
								var p = i.slice,
									d = i.concat,
									f = i.push,
									m = Math.max,
									y = this;
								if (!n(y)) throw new TypeError("Function.prototype.bind called on incompatible " + y);
								for (var h, b = p.call(arguments, 1), v = m(0, y.length - b.length), w = [], E = 0; E < v; E++) f.call(w, "$" + E);
								return h = Function("binder", "return function (" + w.join(",") + "){ return binder.apply(this, arguments); }")(function() {
									if (this instanceof h) {
										var e = y.apply(this, d.call(b, p.call(arguments)));
										return o(e) === e ? e : this
									}
									return y.apply(t, d.call(b, p.call(arguments)))
								}), y.prototype && (c.prototype = y.prototype, h.prototype = new c, c.prototype = null), h
							}
						})
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Window" in this || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && function(e) {
							e.constructor ? e.Window = e.constructor : (e.Window = e.constructor = new Function("return function Window() {}")()).prototype = this
						}(this)
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Document" in this || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && (this.HTMLDocument ? this.Document = this.HTMLDocument : (this.Document = this.HTMLDocument = document.constructor = new Function("return function Document() {}")(), this.Document.prototype = document))
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Element" in this && "HTMLElement" in this || function() {
							if (!window.Element || window.HTMLElement) {
								window.Element = window.HTMLElement = new Function("return function Element() {}")();
								var e, t = document.appendChild(document.createElement("body")),
									n = t.appendChild(document.createElement("iframe")).contentWindow.document,
									a = Element.prototype = n.appendChild(n.createElement("*")),
									s = {},
									l = function(e, t) {
										var n, o, r, i = e.childNodes || [],
											c = -1;
										if (1 === e.nodeType && e.constructor !== Element)
											for (n in e.constructor = Element, s) o = s[n], e[n] = o;
										for (; r = t && i[++c];) l(r, t);
										return e
									},
									u = document.getElementsByTagName("*"),
									o = document.createElement,
									r = 100;
								a.attachEvent("onpropertychange", function(e) {
									for (var t, n = e.propertyName, o = !s.hasOwnProperty(n), r = a[n], i = s[n], c = -1; t = u[++c];) 1 === t.nodeType && (o || t[n] === i) && (t[n] = r);
									s[n] = r
								}), a.constructor = Element, a.hasAttribute || (a.hasAttribute = function(e) {
									return null !== this.getAttribute(e)
								}), i() || (document.onreadystatechange = i, e = setInterval(i, 25)), document.createElement = function(e) {
									var t = o(String(e).toLowerCase());
									return l(t)
								}, document.removeChild(t)
							} else window.HTMLElement = window.Element;

							function i() {
								return r-- || clearTimeout(e), !(!document.body || document.body.prototype || !/(complete|interactive)/.test(document.readyState) || (l(document, !0), e && document.body.prototype && clearTimeout(e), !document.body.prototype))
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(i) {
						(function(e) {
							if (!("Event" in e)) return !1;
							if ("function" == typeof e.Event) return !0;
							try {
								return new Event("click"), !0
							} catch (t) {
								return !1
							}
						})(this) || function() {
							var n = {
								click: 1,
								dblclick: 1,
								keyup: 1,
								keypress: 1,
								keydown: 1,
								mousedown: 1,
								mouseup: 1,
								mousemove: 1,
								mouseover: 1,
								mouseenter: 1,
								mouseleave: 1,
								mouseout: 1,
								storage: 1,
								storagecommit: 1,
								textinput: 1
							};
							if ("undefined" != typeof document && "undefined" != typeof window) {
								var e = window.Event && window.Event.prototype || null;
								window.Event = Window.prototype.Event = function(e, t) {
									if (!e) throw new Error("Not enough arguments");
									var n;
									if ("createEvent" in document) {
										n = document.createEvent("Event");
										var o = !(!t || t.bubbles === i) && t.bubbles,
											r = !(!t || t.cancelable === i) && t.cancelable;
										return n.initEvent(e, o, r), n
									}
									return (n = document.createEventObject()).type = e, n.bubbles = !(!t || t.bubbles === i) && t.bubbles, n.cancelable = !(!t || t.cancelable === i) && t.cancelable, n
								}, e && Object.defineProperty(window.Event, "prototype", {
									configurable: !1,
									enumerable: !1,
									writable: !0,
									value: e
								}), "createEvent" in document || (window.addEventListener = Window.prototype.addEventListener = Document.prototype.addEventListener = Element.prototype.addEventListener = function() {
									var c = this,
										e = arguments[0],
										t = arguments[1];
									if (c === window && e in n) throw new Error("In IE8 the event: " + e + " is not available on the window object. Please see https://github.com/Financial-Times/polyfill-service/issues/317 for more information.");
									c._events || (c._events = {}), c._events[e] || (c._events[e] = function(e) {
										var t, n = c._events[e.type].list,
											o = n.slice(),
											r = -1,
											i = o.length;
										for (e.preventDefault = function() {
											!1 !== e.cancelable && (e.returnValue = !1)
										}, e.stopPropagation = function() {
											e.cancelBubble = !0
										}, e.stopImmediatePropagation = function() {
											e.cancelBubble = !0, e.cancelImmediate = !0
										}, e.currentTarget = c, e.relatedTarget = e.fromElement || null, e.target = e.target || e.srcElement || c, e.timeStamp = (new Date).getTime(), e.clientX && (e.pageX = e.clientX + document.documentElement.scrollLeft, e.pageY = e.clientY + document.documentElement.scrollTop); ++r < i && !e.cancelImmediate;) r in o && -1 !== a(n, t = o[r]) && "function" == typeof t && t.call(c, e)
									}, c._events[e].list = [], c.attachEvent && c.attachEvent("on" + e, c._events[e])), c._events[e].list.push(t)
								}, window.removeEventListener = Window.prototype.removeEventListener = Document.prototype.removeEventListener = Element.prototype.removeEventListener = function() {
									var e, t = this,
										n = arguments[0],
										o = arguments[1];
									t._events && t._events[n] && t._events[n].list && -1 !== (e = a(t._events[n].list, o)) && (t._events[n].list.splice(e, 1), t._events[n].list.length || (t.detachEvent && t.detachEvent("on" + n, t._events[n]), delete t._events[n]))
								}, window.dispatchEvent = Window.prototype.dispatchEvent = Document.prototype.dispatchEvent = Element.prototype.dispatchEvent = function(e) {
									if (!arguments.length) throw new Error("Not enough arguments");
									if (!e || "string" != typeof e.type) throw new Error("DOM Events Exception 0");
									var t = this,
										n = e.type;
									try {
										if (!e.bubbles) {
											e.cancelBubble = !0;
											var o = function(e) {
												e.cancelBubble = !0, (t || window).detachEvent("on" + n, o)
											};
											this.attachEvent("on" + n, o)
										}
										this.fireEvent("on" + n, e)
									} catch (r) {
										for (e.target = t;
											 "_events" in (e.currentTarget = t) && "function" == typeof t._events[n] && t._events[n].call(t, e), "function" == typeof t["on" + n] && t["on" + n].call(t, e), (t = 9 === t.nodeType ? t.parentWindow : t.parentNode) && !e.cancelBubble;);
									}
									return !0
								}, document.attachEvent("onreadystatechange", function() {
									"complete" === document.readyState && document.dispatchEvent(new Event("DOMContentLoaded", {
										bubbles: !0
									}))
								}))
							}

							function a(e, t) {
								for (var n = -1, o = e.length; ++n < o;)
									if (n in e && e[n] === t) return n;
								return -1
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(o) {
						var e, t, n;
						"DOMTokenList" in this && (!("classList" in (e = document.createElement("x"))) || !e.classList.toggle("x", !1) && !e.className) || ("DOMTokenList" in (t = this) && t.DOMTokenList && (!document.createElementNS || !document.createElementNS("http://www.w3.org/2000/svg", "svg") || document.createElementNS("http://www.w3.org/2000/svg", "svg").classList instanceof DOMTokenList) || (t.DOMTokenList = function() {
							var r = !0,
								n = function(e, t, n, o) {
									Object.defineProperty ? Object.defineProperty(e, t, {
										configurable: !1 === r || !!o,
										get: n
									}) : e.__defineGetter__(t, n)
								};
							try {
								n({}, "support")
							} catch (e) {
								r = !1
							}
							return function(r, i) {
								var c = this,
									a = [],
									s = {},
									l = 0,
									e = 0,
									t = function(e) {
										n(c, e, function() {
											return p(), a[e]
										}, !1)
									},
									u = function() {
										if (e <= l)
											for (; e < l; ++e) t(e)
									},
									p = function() {
										var e, t, n = arguments,
											o = /\s+/;
										if (n.length)
											for (t = 0; t < n.length; ++t)
												if (o.test(n[t])) throw (e = new SyntaxError('String "' + n[t] + '" contains an invalid character')).code = 5, e.name = "InvalidCharacterError", e;
										for ("" === (a = "object" == typeof r[i] ? ("" + r[i].baseVal).replace(/^\s+|\s+$/g, "").split(o) : ("" + r[i]).replace(/^\s+|\s+$/g, "").split(o))[0] && (a = []), s = {}, t = 0; t < a.length; ++t) s[a[t]] = !0;
										l = a.length, u()
									};
								return p(), n(c, "length", function() {
									return p(), l
								}), c.toLocaleString = c.toString = function() {
									return p(), a.join(" ")
								}, c.item = function(e) {
									return p(), a[e]
								}, c.contains = function(e) {
									return p(), !!s[e]
								}, c.add = function() {
									p.apply(c, e = arguments);
									for (var e, t, n = 0, o = e.length; n < o; ++n) t = e[n], s[t] || (a.push(t), s[t] = !0);
									l !== a.length && (l = a.length >>> 0, "object" == typeof r[i] ? r[i].baseVal = a.join(" ") : r[i] = a.join(" "), u())
								}, c.remove = function() {
									p.apply(c, e = arguments);
									for (var e, t = {}, n = 0, o = []; n < e.length; ++n) t[e[n]] = !0, delete s[e[n]];
									for (n = 0; n < a.length; ++n) t[a[n]] || o.push(a[n]);
									l = (a = o).length >>> 0, "object" == typeof r[i] ? r[i].baseVal = a.join(" ") : r[i] = a.join(" "), u()
								}, c.toggle = function(e, t) {
									return p.apply(c, [e]), o !== t ? t ? (c.add(e), !0) : (c.remove(e), !1) : s[e] ? (c.remove(e), !1) : (c.add(e), !0)
								}, c
							}
						}()), "classList" in (n = document.createElement("span")) && (n.classList.toggle("x", !1), n.classList.contains("x") && (n.classList.constructor.prototype.toggle = function(e) {
							var t = arguments[1];
							if (t !== o) return this[(t = !!t) ? "add" : "remove"](e), t;
							var n = !this.contains(e);
							return this[n ? "add" : "remove"](e), n
						})), function() {
							var e = document.createElement("span");
							if ("classList" in e && (e.classList.add("a", "b"), !e.classList.contains("b"))) {
								var o = e.classList.constructor.prototype.add;
								e.classList.constructor.prototype.add = function() {
									for (var e = arguments, t = arguments.length, n = 0; n < t; n++) o.call(this, e[n])
								}
							}
						}(), function() {
							var e = document.createElement("span");
							if ("classList" in e && (e.classList.add("a"), e.classList.add("b"), e.classList.remove("a", "b"), e.classList.contains("b"))) {
								var o = e.classList.constructor.prototype.remove;
								e.classList.constructor.prototype.remove = function() {
									for (var e = arguments, t = arguments.length, n = 0; n < t; n++) o.call(this, e[n])
								}
							}
						}())
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						var t;
						"document" in this && "classList" in document.documentElement && "Element" in this && "classList" in Element.prototype && ((t = document.createElement("span")).classList.add("a", "b"), t.classList.contains("b")) || function(e) {
							var u = !0,
								p = function(e, t, n, o) {
									Object.defineProperty ? Object.defineProperty(e, t, {
										configurable: !1 === u || !!o,
										get: n
									}) : e.__defineGetter__(t, n)
								};
							try {
								p({}, "support")
							} catch (t) {
								u = !1
							}
							var d = function(e, s, l) {
								p(e.prototype, s, function() {
									var e, t = this,
										n = "__defineGetter__DEFINE_PROPERTY" + s;
									if (t[n]) return e;
									if (!(t[n] = !0) === u) {
										for (var o, r = d.mirror || document.createElement("div"), i = r.childNodes, c = i.length, a = 0; a < c; ++a)
											if (i[a]._R === t) {
												o = i[a];
												break
											} o || (o = r.appendChild(document.createElement("div"))), e = DOMTokenList.call(o, t, l)
									} else e = new DOMTokenList(t, l);
									return p(t, s, function() {
										return e
									}), delete t[n], e
								}, !0)
							};
							d(e.Element, "classList", "className"), d(e.HTMLElement, "classList", "className"), d(e.HTMLLinkElement, "relList", "rel"), d(e.HTMLAnchorElement, "relList", "rel"), d(e.HTMLAreaElement, "relList", "rel")
						}(this)
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}), e.prototype.defaults = {
					characterCountAttribute: "data-maxlength",
					wordCountAttribute: "data-maxwords"
				}, e.prototype.init = function() {
					var e = this.$module,
						t = this.$textarea,
						n = this.$countMessage;
					if (t && n) {
						t.insertAdjacentElement("afterend", n), this.options = this.getDataset(e);
						var o = this.defaults.characterCountAttribute;
						this.options.maxwords && (o = this.defaults.wordCountAttribute), this.maxLength = e.getAttribute(o), this.maxLength && (e.removeAttribute("maxlength"), this.bindChangeEvents.bind(this)(), this.updateCountMessage.bind(this)())
					}
				}, e.prototype.getDataset = function(e) {
					var t = {},
						n = e.attributes;
					if (n)
						for (var o = 0; o < n.length; o++) {
							var r = n[o],
								i = r.name.match(/^data-(.+)/);
							i && (t[i[1]] = r.value)
						}
					return t
				}, e.prototype.count = function(e) {
					return this.options.maxwords ? (e.match(/\S+/g) || []).length : e.length
				}, e.prototype.bindChangeEvents = function() {
					var e = this.$textarea;
					e.addEventListener("keyup", this.checkIfValueChanged.bind(this)), e.addEventListener("focus", this.handleFocus.bind(this)), e.addEventListener("blur", this.handleBlur.bind(this))
				}, e.prototype.checkIfValueChanged = function() {
					this.$textarea.oldValue || (this.$textarea.oldValue = ""), this.$textarea.value !== this.$textarea.oldValue && (this.$textarea.oldValue = this.$textarea.value, this.updateCountMessage.bind(this)())
				}, e.prototype.updateCountMessage = function() {
					var e = this.$textarea,
						t = this.options,
						n = this.$countMessage,
						o = this.count(e.value),
						r = this.maxLength,
						i = r - o;
					o < r * (t.threshold ? t.threshold : 0) / 100 ? (n.classList.add("govuk-character-count__message--disabled"), n.setAttribute("aria-hidden", !0)) : (n.classList.remove("govuk-character-count__message--disabled"), n.removeAttribute("aria-hidden")), i < 0 ? (e.classList.add("govuk-textarea--error"), n.classList.remove("govuk-hint"), n.classList.add("govuk-error-message")) : (e.classList.remove("govuk-textarea--error"), n.classList.remove("govuk-error-message"), n.classList.add("govuk-hint"));
					var c, a, s = "character";
					t.maxwords && (s = "word"), s += -1 == i || 1 == i ? "" : "s", c = i < 0 ? "too many" : "remaining", a = Math.abs(i), n.innerHTML = "You have " + a + " " + s + " " + c
				}, e.prototype.handleFocus = function() {
					this.valueChecker = setInterval(this.checkIfValueChanged.bind(this), 1e3)
				}, e.prototype.handleBlur = function() {
					clearInterval(this.valueChecker)
				}, e
			}()
		}),
		a = e(function(e, t) {
			e.exports = function() {
				function e(e) {
					this.$module = e, this.$inputs = e.querySelectorAll('input[type="checkbox"]')
				}
				return function(e) {
					var a, s, l, u;
					"defineProperty" in Object && function() {
						try {
							return Object.defineProperty({}, "test", {
								value: 42
							}), !0
						} catch (e) {
							return !1
						}
					}() || (a = Object.defineProperty, s = Object.prototype.hasOwnProperty("__defineGetter__"), l = "Getters & setters cannot be defined on this javascript engine", u = "A property cannot both have accessors and be writable or have a value", Object.defineProperty = function(e, t, n) {
						if (a && (e === window || e === document || e === Element.prototype || e instanceof Element)) return a(e, t, n);
						if (null === e || !(e instanceof Object || "object" == typeof e)) throw new TypeError("Object.defineProperty called on non-object");
						if (!(n instanceof Object)) throw new TypeError("Property description must be an object");
						var o = String(t),
							r = "value" in n || "writable" in n,
							i = "get" in n && typeof n.get,
							c = "set" in n && typeof n.set;
						if (i) {
							if ("function" !== i) throw new TypeError("Getter must be a function");
							if (!s) throw new TypeError(l);
							if (r) throw new TypeError(u);
							Object.__defineGetter__.call(e, o, n.get)
						} else e[o] = n.value;
						if (c) {
							if ("function" !== c) throw new TypeError("Setter must be a function");
							if (!s) throw new TypeError(l);
							if (r) throw new TypeError(u);
							Object.__defineSetter__.call(e, o, n.set)
						}
						return "value" in n && (e[o] = n.value), e
					})
				}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"bind" in Function.prototype || Object.defineProperty(Function.prototype, "bind", {
							value: function(t) {
								var n, e = Array,
									o = Object,
									r = o.prototype,
									i = e.prototype,
									c = function c() {},
									a = r.toString,
									s = "function" == typeof Symbol && "symbol" == typeof Symbol.toStringTag,
									l = Function.prototype.toString,
									u = function u(e) {
										try {
											return l.call(e), !0
										} catch (t) {
											return !1
										}
									};
								n = function n(e) {
									if ("function" != typeof e) return !1;
									if (s) return u(e);
									var t = a.call(e);
									return "[object Function]" === t || "[object GeneratorFunction]" === t
								};
								var p = i.slice,
									d = i.concat,
									f = i.push,
									m = Math.max,
									y = this;
								if (!n(y)) throw new TypeError("Function.prototype.bind called on incompatible " + y);
								for (var h, b = p.call(arguments, 1), v = m(0, y.length - b.length), w = [], E = 0; E < v; E++) f.call(w, "$" + E);
								return h = Function("binder", "return function (" + w.join(",") + "){ return binder.apply(this, arguments); }")(function() {
									if (this instanceof h) {
										var e = y.apply(this, d.call(b, p.call(arguments)));
										return o(e) === e ? e : this
									}
									return y.apply(t, d.call(b, p.call(arguments)))
								}), y.prototype && (c.prototype = y.prototype, h.prototype = new c, c.prototype = null), h
							}
						})
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Window" in this || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && function(e) {
							e.constructor ? e.Window = e.constructor : (e.Window = e.constructor = new Function("return function Window() {}")()).prototype = this
						}(this)
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Document" in this || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && (this.HTMLDocument ? this.Document = this.HTMLDocument : (this.Document = this.HTMLDocument = document.constructor = new Function("return function Document() {}")(), this.Document.prototype = document))
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Element" in this && "HTMLElement" in this || function() {
							if (!window.Element || window.HTMLElement) {
								window.Element = window.HTMLElement = new Function("return function Element() {}")();
								var e, t = document.appendChild(document.createElement("body")),
									n = t.appendChild(document.createElement("iframe")).contentWindow.document,
									a = Element.prototype = n.appendChild(n.createElement("*")),
									s = {},
									l = function(e, t) {
										var n, o, r, i = e.childNodes || [],
											c = -1;
										if (1 === e.nodeType && e.constructor !== Element)
											for (n in e.constructor = Element, s) o = s[n], e[n] = o;
										for (; r = t && i[++c];) l(r, t);
										return e
									},
									u = document.getElementsByTagName("*"),
									o = document.createElement,
									r = 100;
								a.attachEvent("onpropertychange", function(e) {
									for (var t, n = e.propertyName, o = !s.hasOwnProperty(n), r = a[n], i = s[n], c = -1; t = u[++c];) 1 === t.nodeType && (o || t[n] === i) && (t[n] = r);
									s[n] = r
								}), a.constructor = Element, a.hasAttribute || (a.hasAttribute = function(e) {
									return null !== this.getAttribute(e)
								}), i() || (document.onreadystatechange = i, e = setInterval(i, 25)), document.createElement = function(e) {
									var t = o(String(e).toLowerCase());
									return l(t)
								}, document.removeChild(t)
							} else window.HTMLElement = window.Element;

							function i() {
								return r-- || clearTimeout(e), !(!document.body || document.body.prototype || !/(complete|interactive)/.test(document.readyState) || (l(document, !0), e && document.body.prototype && clearTimeout(e), !document.body.prototype))
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(i) {
						(function(e) {
							if (!("Event" in e)) return !1;
							if ("function" == typeof e.Event) return !0;
							try {
								return new Event("click"), !0
							} catch (t) {
								return !1
							}
						})(this) || function() {
							var n = {
								click: 1,
								dblclick: 1,
								keyup: 1,
								keypress: 1,
								keydown: 1,
								mousedown: 1,
								mouseup: 1,
								mousemove: 1,
								mouseover: 1,
								mouseenter: 1,
								mouseleave: 1,
								mouseout: 1,
								storage: 1,
								storagecommit: 1,
								textinput: 1
							};
							if ("undefined" != typeof document && "undefined" != typeof window) {
								var e = window.Event && window.Event.prototype || null;
								window.Event = Window.prototype.Event = function(e, t) {
									if (!e) throw new Error("Not enough arguments");
									var n;
									if ("createEvent" in document) {
										n = document.createEvent("Event");
										var o = !(!t || t.bubbles === i) && t.bubbles,
											r = !(!t || t.cancelable === i) && t.cancelable;
										return n.initEvent(e, o, r), n
									}
									return (n = document.createEventObject()).type = e, n.bubbles = !(!t || t.bubbles === i) && t.bubbles, n.cancelable = !(!t || t.cancelable === i) && t.cancelable, n
								}, e && Object.defineProperty(window.Event, "prototype", {
									configurable: !1,
									enumerable: !1,
									writable: !0,
									value: e
								}), "createEvent" in document || (window.addEventListener = Window.prototype.addEventListener = Document.prototype.addEventListener = Element.prototype.addEventListener = function() {
									var c = this,
										e = arguments[0],
										t = arguments[1];
									if (c === window && e in n) throw new Error("In IE8 the event: " + e + " is not available on the window object. Please see https://github.com/Financial-Times/polyfill-service/issues/317 for more information.");
									c._events || (c._events = {}), c._events[e] || (c._events[e] = function(e) {
										var t, n = c._events[e.type].list,
											o = n.slice(),
											r = -1,
											i = o.length;
										for (e.preventDefault = function() {
											!1 !== e.cancelable && (e.returnValue = !1)
										}, e.stopPropagation = function() {
											e.cancelBubble = !0
										}, e.stopImmediatePropagation = function() {
											e.cancelBubble = !0, e.cancelImmediate = !0
										}, e.currentTarget = c, e.relatedTarget = e.fromElement || null, e.target = e.target || e.srcElement || c, e.timeStamp = (new Date).getTime(), e.clientX && (e.pageX = e.clientX + document.documentElement.scrollLeft, e.pageY = e.clientY + document.documentElement.scrollTop); ++r < i && !e.cancelImmediate;) r in o && -1 !== a(n, t = o[r]) && "function" == typeof t && t.call(c, e)
									}, c._events[e].list = [], c.attachEvent && c.attachEvent("on" + e, c._events[e])), c._events[e].list.push(t)
								}, window.removeEventListener = Window.prototype.removeEventListener = Document.prototype.removeEventListener = Element.prototype.removeEventListener = function() {
									var e, t = this,
										n = arguments[0],
										o = arguments[1];
									t._events && t._events[n] && t._events[n].list && -1 !== (e = a(t._events[n].list, o)) && (t._events[n].list.splice(e, 1), t._events[n].list.length || (t.detachEvent && t.detachEvent("on" + n, t._events[n]), delete t._events[n]))
								}, window.dispatchEvent = Window.prototype.dispatchEvent = Document.prototype.dispatchEvent = Element.prototype.dispatchEvent = function(e) {
									if (!arguments.length) throw new Error("Not enough arguments");
									if (!e || "string" != typeof e.type) throw new Error("DOM Events Exception 0");
									var t = this,
										n = e.type;
									try {
										if (!e.bubbles) {
											e.cancelBubble = !0;
											var o = function(e) {
												e.cancelBubble = !0, (t || window).detachEvent("on" + n, o)
											};
											this.attachEvent("on" + n, o)
										}
										this.fireEvent("on" + n, e)
									} catch (r) {
										for (e.target = t;
											 "_events" in (e.currentTarget = t) && "function" == typeof t._events[n] && t._events[n].call(t, e), "function" == typeof t["on" + n] && t["on" + n].call(t, e), (t = 9 === t.nodeType ? t.parentWindow : t.parentNode) && !e.cancelBubble;);
									}
									return !0
								}, document.attachEvent("onreadystatechange", function() {
									"complete" === document.readyState && document.dispatchEvent(new Event("DOMContentLoaded", {
										bubbles: !0
									}))
								}))
							}

							function a(e, t) {
								for (var n = -1, o = e.length; ++n < o;)
									if (n in e && e[n] === t) return n;
								return -1
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(o) {
						var e, t, n;
						"DOMTokenList" in this && (!("classList" in (e = document.createElement("x"))) || !e.classList.toggle("x", !1) && !e.className) || ("DOMTokenList" in (t = this) && t.DOMTokenList && (!document.createElementNS || !document.createElementNS("http://www.w3.org/2000/svg", "svg") || document.createElementNS("http://www.w3.org/2000/svg", "svg").classList instanceof DOMTokenList) || (t.DOMTokenList = function() {
							var r = !0,
								n = function(e, t, n, o) {
									Object.defineProperty ? Object.defineProperty(e, t, {
										configurable: !1 === r || !!o,
										get: n
									}) : e.__defineGetter__(t, n)
								};
							try {
								n({}, "support")
							} catch (e) {
								r = !1
							}
							return function(r, i) {
								var c = this,
									a = [],
									s = {},
									l = 0,
									e = 0,
									t = function(e) {
										n(c, e, function() {
											return p(), a[e]
										}, !1)
									},
									u = function() {
										if (e <= l)
											for (; e < l; ++e) t(e)
									},
									p = function() {
										var e, t, n = arguments,
											o = /\s+/;
										if (n.length)
											for (t = 0; t < n.length; ++t)
												if (o.test(n[t])) throw (e = new SyntaxError('String "' + n[t] + '" contains an invalid character')).code = 5, e.name = "InvalidCharacterError", e;
										for ("" === (a = "object" == typeof r[i] ? ("" + r[i].baseVal).replace(/^\s+|\s+$/g, "").split(o) : ("" + r[i]).replace(/^\s+|\s+$/g, "").split(o))[0] && (a = []), s = {}, t = 0; t < a.length; ++t) s[a[t]] = !0;
										l = a.length, u()
									};
								return p(), n(c, "length", function() {
									return p(), l
								}), c.toLocaleString = c.toString = function() {
									return p(), a.join(" ")
								}, c.item = function(e) {
									return p(), a[e]
								}, c.contains = function(e) {
									return p(), !!s[e]
								}, c.add = function() {
									p.apply(c, e = arguments);
									for (var e, t, n = 0, o = e.length; n < o; ++n) t = e[n], s[t] || (a.push(t), s[t] = !0);
									l !== a.length && (l = a.length >>> 0, "object" == typeof r[i] ? r[i].baseVal = a.join(" ") : r[i] = a.join(" "), u())
								}, c.remove = function() {
									p.apply(c, e = arguments);
									for (var e, t = {}, n = 0, o = []; n < e.length; ++n) t[e[n]] = !0, delete s[e[n]];
									for (n = 0; n < a.length; ++n) t[a[n]] || o.push(a[n]);
									l = (a = o).length >>> 0, "object" == typeof r[i] ? r[i].baseVal = a.join(" ") : r[i] = a.join(" "), u()
								}, c.toggle = function(e, t) {
									return p.apply(c, [e]), o !== t ? t ? (c.add(e), !0) : (c.remove(e), !1) : s[e] ? (c.remove(e), !1) : (c.add(e), !0)
								}, c
							}
						}()), "classList" in (n = document.createElement("span")) && (n.classList.toggle("x", !1), n.classList.contains("x") && (n.classList.constructor.prototype.toggle = function(e) {
							var t = arguments[1];
							if (t !== o) return this[(t = !!t) ? "add" : "remove"](e), t;
							var n = !this.contains(e);
							return this[n ? "add" : "remove"](e), n
						})), function() {
							var e = document.createElement("span");
							if ("classList" in e && (e.classList.add("a", "b"), !e.classList.contains("b"))) {
								var o = e.classList.constructor.prototype.add;
								e.classList.constructor.prototype.add = function() {
									for (var e = arguments, t = arguments.length, n = 0; n < t; n++) o.call(this, e[n])
								}
							}
						}(), function() {
							var e = document.createElement("span");
							if ("classList" in e && (e.classList.add("a"), e.classList.add("b"), e.classList.remove("a", "b"), e.classList.contains("b"))) {
								var o = e.classList.constructor.prototype.remove;
								e.classList.constructor.prototype.remove = function() {
									for (var e = arguments, t = arguments.length, n = 0; n < t; n++) o.call(this, e[n])
								}
							}
						}())
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						var t;
						"document" in this && "classList" in document.documentElement && "Element" in this && "classList" in Element.prototype && ((t = document.createElement("span")).classList.add("a", "b"), t.classList.contains("b")) || function(e) {
							var u = !0,
								p = function(e, t, n, o) {
									Object.defineProperty ? Object.defineProperty(e, t, {
										configurable: !1 === u || !!o,
										get: n
									}) : e.__defineGetter__(t, n)
								};
							try {
								p({}, "support")
							} catch (t) {
								u = !1
							}
							var d = function(e, s, l) {
								p(e.prototype, s, function() {
									var e, t = this,
										n = "__defineGetter__DEFINE_PROPERTY" + s;
									if (t[n]) return e;
									if (!(t[n] = !0) === u) {
										for (var o, r = d.mirror || document.createElement("div"), i = r.childNodes, c = i.length, a = 0; a < c; ++a)
											if (i[a]._R === t) {
												o = i[a];
												break
											} o || (o = r.appendChild(document.createElement("div"))), e = DOMTokenList.call(o, t, l)
									} else e = new DOMTokenList(t, l);
									return p(t, s, function() {
										return e
									}), delete t[n], e
								}, !0)
							};
							d(e.Element, "classList", "className"), d(e.HTMLElement, "classList", "className"), d(e.HTMLLinkElement, "relList", "rel"), d(e.HTMLAnchorElement, "relList", "rel"), d(e.HTMLAreaElement, "relList", "rel")
						}(this)
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}), e.prototype.init = function() {
					var n = this.$module;
					! function o(e, t) {
						if (window.NodeList.prototype.forEach) return e.forEach(t);
						for (var n = 0; n < e.length; n++) t.call(window, e[n], n, e)
					}(this.$inputs, function(e) {
						var t = e.getAttribute("data-aria-controls");
						t && n.querySelector("#" + t) && (e.setAttribute("aria-controls", t), e.removeAttribute("data-aria-controls"), this.setAttributes(e))
					}.bind(this)), n.addEventListener("click", this.handleClick.bind(this))
				}, e.prototype.setAttributes = function(e) {
					var t = e.checked;
					e.setAttribute("aria-expanded", t);
					var n = this.$module.querySelector("#" + e.getAttribute("aria-controls"));
					n && n.classList.toggle("govuk-checkboxes__conditional--hidden", !t)
				}, e.prototype.handleClick = function(e) {
					var t = e.target,
						n = "checkbox" === t.getAttribute("type"),
						o = t.getAttribute("aria-controls");
					n && o && this.setAttributes(t)
				}, e
			}()
		}),
		s = e(function(e, t) {
			e.exports = function() {
				function e(e) {
					this.$module = e
				}
				return function(e) {
					var a, s, l, u;
					"defineProperty" in Object && function() {
						try {
							return Object.defineProperty({}, "test", {
								value: 42
							}), !0
						} catch (e) {
							return !1
						}
					}() || (a = Object.defineProperty, s = Object.prototype.hasOwnProperty("__defineGetter__"), l = "Getters & setters cannot be defined on this javascript engine", u = "A property cannot both have accessors and be writable or have a value", Object.defineProperty = function(e, t, n) {
						if (a && (e === window || e === document || e === Element.prototype || e instanceof Element)) return a(e, t, n);
						if (null === e || !(e instanceof Object || "object" == typeof e)) throw new TypeError("Object.defineProperty called on non-object");
						if (!(n instanceof Object)) throw new TypeError("Property description must be an object");
						var o = String(t),
							r = "value" in n || "writable" in n,
							i = "get" in n && typeof n.get,
							c = "set" in n && typeof n.set;
						if (i) {
							if ("function" !== i) throw new TypeError("Getter must be a function");
							if (!s) throw new TypeError(l);
							if (r) throw new TypeError(u);
							Object.__defineGetter__.call(e, o, n.get)
						} else e[o] = n.value;
						if (c) {
							if ("function" !== c) throw new TypeError("Setter must be a function");
							if (!s) throw new TypeError(l);
							if (r) throw new TypeError(u);
							Object.__defineSetter__.call(e, o, n.set)
						}
						return "value" in n && (e[o] = n.value), e
					})
				}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"bind" in Function.prototype || Object.defineProperty(Function.prototype, "bind", {
							value: function(t) {
								var n, e = Array,
									o = Object,
									r = o.prototype,
									i = e.prototype,
									c = function c() {},
									a = r.toString,
									s = "function" == typeof Symbol && "symbol" == typeof Symbol.toStringTag,
									l = Function.prototype.toString,
									u = function u(e) {
										try {
											return l.call(e), !0
										} catch (t) {
											return !1
										}
									};
								n = function n(e) {
									if ("function" != typeof e) return !1;
									if (s) return u(e);
									var t = a.call(e);
									return "[object Function]" === t || "[object GeneratorFunction]" === t
								};
								var p = i.slice,
									d = i.concat,
									f = i.push,
									m = Math.max,
									y = this;
								if (!n(y)) throw new TypeError("Function.prototype.bind called on incompatible " + y);
								for (var h, b = p.call(arguments, 1), v = m(0, y.length - b.length), w = [], E = 0; E < v; E++) f.call(w, "$" + E);
								return h = Function("binder", "return function (" + w.join(",") + "){ return binder.apply(this, arguments); }")(function() {
									if (this instanceof h) {
										var e = y.apply(this, d.call(b, p.call(arguments)));
										return o(e) === e ? e : this
									}
									return y.apply(t, d.call(b, p.call(arguments)))
								}), y.prototype && (c.prototype = y.prototype, h.prototype = new c, c.prototype = null), h
							}
						})
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Window" in this || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && function(e) {
							e.constructor ? e.Window = e.constructor : (e.Window = e.constructor = new Function("return function Window() {}")()).prototype = this
						}(this)
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Document" in this || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && (this.HTMLDocument ? this.Document = this.HTMLDocument : (this.Document = this.HTMLDocument = document.constructor = new Function("return function Document() {}")(), this.Document.prototype = document))
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Element" in this && "HTMLElement" in this || function() {
							if (!window.Element || window.HTMLElement) {
								window.Element = window.HTMLElement = new Function("return function Element() {}")();
								var e, t = document.appendChild(document.createElement("body")),
									n = t.appendChild(document.createElement("iframe")).contentWindow.document,
									a = Element.prototype = n.appendChild(n.createElement("*")),
									s = {},
									l = function(e, t) {
										var n, o, r, i = e.childNodes || [],
											c = -1;
										if (1 === e.nodeType && e.constructor !== Element)
											for (n in e.constructor = Element, s) o = s[n], e[n] = o;
										for (; r = t && i[++c];) l(r, t);
										return e
									},
									u = document.getElementsByTagName("*"),
									o = document.createElement,
									r = 100;
								a.attachEvent("onpropertychange", function(e) {
									for (var t, n = e.propertyName, o = !s.hasOwnProperty(n), r = a[n], i = s[n], c = -1; t = u[++c];) 1 === t.nodeType && (o || t[n] === i) && (t[n] = r);
									s[n] = r
								}), a.constructor = Element, a.hasAttribute || (a.hasAttribute = function(e) {
									return null !== this.getAttribute(e)
								}), i() || (document.onreadystatechange = i, e = setInterval(i, 25)), document.createElement = function(e) {
									var t = o(String(e).toLowerCase());
									return l(t)
								}, document.removeChild(t)
							} else window.HTMLElement = window.Element;

							function i() {
								return r-- || clearTimeout(e), !(!document.body || document.body.prototype || !/(complete|interactive)/.test(document.readyState) || (l(document, !0), e && document.body.prototype && clearTimeout(e), !document.body.prototype))
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(i) {
						(function(e) {
							if (!("Event" in e)) return !1;
							if ("function" == typeof e.Event) return !0;
							try {
								return new Event("click"), !0
							} catch (t) {
								return !1
							}
						})(this) || function() {
							var n = {
								click: 1,
								dblclick: 1,
								keyup: 1,
								keypress: 1,
								keydown: 1,
								mousedown: 1,
								mouseup: 1,
								mousemove: 1,
								mouseover: 1,
								mouseenter: 1,
								mouseleave: 1,
								mouseout: 1,
								storage: 1,
								storagecommit: 1,
								textinput: 1
							};
							if ("undefined" != typeof document && "undefined" != typeof window) {
								var e = window.Event && window.Event.prototype || null;
								window.Event = Window.prototype.Event = function(e, t) {
									if (!e) throw new Error("Not enough arguments");
									var n;
									if ("createEvent" in document) {
										n = document.createEvent("Event");
										var o = !(!t || t.bubbles === i) && t.bubbles,
											r = !(!t || t.cancelable === i) && t.cancelable;
										return n.initEvent(e, o, r), n
									}
									return (n = document.createEventObject()).type = e, n.bubbles = !(!t || t.bubbles === i) && t.bubbles, n.cancelable = !(!t || t.cancelable === i) && t.cancelable, n
								}, e && Object.defineProperty(window.Event, "prototype", {
									configurable: !1,
									enumerable: !1,
									writable: !0,
									value: e
								}), "createEvent" in document || (window.addEventListener = Window.prototype.addEventListener = Document.prototype.addEventListener = Element.prototype.addEventListener = function() {
									var c = this,
										e = arguments[0],
										t = arguments[1];
									if (c === window && e in n) throw new Error("In IE8 the event: " + e + " is not available on the window object. Please see https://github.com/Financial-Times/polyfill-service/issues/317 for more information.");
									c._events || (c._events = {}), c._events[e] || (c._events[e] = function(e) {
										var t, n = c._events[e.type].list,
											o = n.slice(),
											r = -1,
											i = o.length;
										for (e.preventDefault = function() {
											!1 !== e.cancelable && (e.returnValue = !1)
										}, e.stopPropagation = function() {
											e.cancelBubble = !0
										}, e.stopImmediatePropagation = function() {
											e.cancelBubble = !0, e.cancelImmediate = !0
										}, e.currentTarget = c, e.relatedTarget = e.fromElement || null, e.target = e.target || e.srcElement || c, e.timeStamp = (new Date).getTime(), e.clientX && (e.pageX = e.clientX + document.documentElement.scrollLeft, e.pageY = e.clientY + document.documentElement.scrollTop); ++r < i && !e.cancelImmediate;) r in o && -1 !== a(n, t = o[r]) && "function" == typeof t && t.call(c, e)
									}, c._events[e].list = [], c.attachEvent && c.attachEvent("on" + e, c._events[e])), c._events[e].list.push(t)
								}, window.removeEventListener = Window.prototype.removeEventListener = Document.prototype.removeEventListener = Element.prototype.removeEventListener = function() {
									var e, t = this,
										n = arguments[0],
										o = arguments[1];
									t._events && t._events[n] && t._events[n].list && -1 !== (e = a(t._events[n].list, o)) && (t._events[n].list.splice(e, 1), t._events[n].list.length || (t.detachEvent && t.detachEvent("on" + n, t._events[n]), delete t._events[n]))
								}, window.dispatchEvent = Window.prototype.dispatchEvent = Document.prototype.dispatchEvent = Element.prototype.dispatchEvent = function(e) {
									if (!arguments.length) throw new Error("Not enough arguments");
									if (!e || "string" != typeof e.type) throw new Error("DOM Events Exception 0");
									var t = this,
										n = e.type;
									try {
										if (!e.bubbles) {
											e.cancelBubble = !0;
											var o = function(e) {
												e.cancelBubble = !0, (t || window).detachEvent("on" + n, o)
											};
											this.attachEvent("on" + n, o)
										}
										this.fireEvent("on" + n, e)
									} catch (r) {
										for (e.target = t;
											 "_events" in (e.currentTarget = t) && "function" == typeof t._events[n] && t._events[n].call(t, e), "function" == typeof t["on" + n] && t["on" + n].call(t, e), (t = 9 === t.nodeType ? t.parentWindow : t.parentNode) && !e.cancelBubble;);
									}
									return !0
								}, document.attachEvent("onreadystatechange", function() {
									"complete" === document.readyState && document.dispatchEvent(new Event("DOMContentLoaded", {
										bubbles: !0
									}))
								}))
							}

							function a(e, t) {
								for (var n = -1, o = e.length; ++n < o;)
									if (n in e && e[n] === t) return n;
								return -1
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"document" in this && "matches" in document.documentElement || (Element.prototype.matches = Element.prototype.webkitMatchesSelector || Element.prototype.oMatchesSelector || Element.prototype.msMatchesSelector || Element.prototype.mozMatchesSelector || function(e) {
							for (var t = (this.document || this.ownerDocument).querySelectorAll(e), n = 0; t[n] && t[n] !== this;) ++n;
							return !!t[n]
						})
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"document" in this && "closest" in document.documentElement || (Element.prototype.closest = function(e) {
							for (var t = this; t;) {
								if (t.matches(e)) return t;
								t = "SVGElement" in window && t instanceof SVGElement ? t.parentNode : t.parentElement
							}
							return null
						})
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}), e.prototype.init = function() {
					var e = this.$module;
					e && (e.focus(), e.addEventListener("click", this.handleClick.bind(this)))
				}, e.prototype.handleClick = function(e) {
					var t = e.target;
					this.focusTarget(t) && e.preventDefault()
				}, e.prototype.focusTarget = function(e) {
					if ("A" !== e.tagName || !1 === e.href) return !1;
					var t = this.getFragmentFromUrl(e.href),
						n = document.getElementById(t);
					if (!n) return !1;
					var o = this.getAssociatedLegendOrLabel(n);
					return !!o && (o.scrollIntoView(), n.focus({
						preventScroll: !0
					}), !0)
				}, e.prototype.getFragmentFromUrl = function(e) {
					return -1 !== e.indexOf("#") && e.split("#").pop()
				}, e.prototype.getAssociatedLegendOrLabel = function(e) {
					var t = e.closest("fieldset");
					if (t) {
						var n = t.getElementsByTagName("legend");
						if (n.length) {
							var o = n[0];
							if ("checkbox" === e.type || "radio" === e.type) return o;
							var r = o.getBoundingClientRect().top,
								i = e.getBoundingClientRect();
							if (i.height && window.innerHeight && i.top + i.height - r < window.innerHeight / 2) return o
						}
					}
					return document.querySelector("label[for='" + e.getAttribute("id") + "']") || e.closest("label")
				}, e
			}()
		}),
		l = e(function(e, t) {
			e.exports = function() {
				function t(e, t) {
					if (window.NodeList.prototype.forEach) return e.forEach(t);
					for (var n = 0; n < e.length; n++) t.call(window, e[n], n, e)
				}

				function e(e) {
					this.$module = e
				}
				return function(e) {
					var a, s, l, u;
					"defineProperty" in Object && function() {
						try {
							return Object.defineProperty({}, "test", {
								value: 42
							}), !0
						} catch (e) {
							return !1
						}
					}() || (a = Object.defineProperty, s = Object.prototype.hasOwnProperty("__defineGetter__"), l = "Getters & setters cannot be defined on this javascript engine", u = "A property cannot both have accessors and be writable or have a value", Object.defineProperty = function(e, t, n) {
						if (a && (e === window || e === document || e === Element.prototype || e instanceof Element)) return a(e, t, n);
						if (null === e || !(e instanceof Object || "object" == typeof e)) throw new TypeError("Object.defineProperty called on non-object");
						if (!(n instanceof Object)) throw new TypeError("Property description must be an object");
						var o = String(t),
							r = "value" in n || "writable" in n,
							i = "get" in n && typeof n.get,
							c = "set" in n && typeof n.set;
						if (i) {
							if ("function" !== i) throw new TypeError("Getter must be a function");
							if (!s) throw new TypeError(l);
							if (r) throw new TypeError(u);
							Object.__defineGetter__.call(e, o, n.get)
						} else e[o] = n.value;
						if (c) {
							if ("function" !== c) throw new TypeError("Setter must be a function");
							if (!s) throw new TypeError(l);
							if (r) throw new TypeError(u);
							Object.__defineSetter__.call(e, o, n.set)
						}
						return "value" in n && (e[o] = n.value), e
					})
				}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"bind" in Function.prototype || Object.defineProperty(Function.prototype, "bind", {
							value: function(t) {
								var n, e = Array,
									o = Object,
									r = o.prototype,
									i = e.prototype,
									c = function c() {},
									a = r.toString,
									s = "function" == typeof Symbol && "symbol" == typeof Symbol.toStringTag,
									l = Function.prototype.toString,
									u = function u(e) {
										try {
											return l.call(e), !0
										} catch (t) {
											return !1
										}
									};
								n = function n(e) {
									if ("function" != typeof e) return !1;
									if (s) return u(e);
									var t = a.call(e);
									return "[object Function]" === t || "[object GeneratorFunction]" === t
								};
								var p = i.slice,
									d = i.concat,
									f = i.push,
									m = Math.max,
									y = this;
								if (!n(y)) throw new TypeError("Function.prototype.bind called on incompatible " + y);
								for (var h, b = p.call(arguments, 1), v = m(0, y.length - b.length), w = [], E = 0; E < v; E++) f.call(w, "$" + E);
								return h = Function("binder", "return function (" + w.join(",") + "){ return binder.apply(this, arguments); }")(function() {
									if (this instanceof h) {
										var e = y.apply(this, d.call(b, p.call(arguments)));
										return o(e) === e ? e : this
									}
									return y.apply(t, d.call(b, p.call(arguments)))
								}), y.prototype && (c.prototype = y.prototype, h.prototype = new c, c.prototype = null), h
							}
						})
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Window" in this || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && function(e) {
							e.constructor ? e.Window = e.constructor : (e.Window = e.constructor = new Function("return function Window() {}")()).prototype = this
						}(this)
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Document" in this || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && (this.HTMLDocument ? this.Document = this.HTMLDocument : (this.Document = this.HTMLDocument = document.constructor = new Function("return function Document() {}")(), this.Document.prototype = document))
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Element" in this && "HTMLElement" in this || function() {
							if (!window.Element || window.HTMLElement) {
								window.Element = window.HTMLElement = new Function("return function Element() {}")();
								var e, t = document.appendChild(document.createElement("body")),
									n = t.appendChild(document.createElement("iframe")).contentWindow.document,
									a = Element.prototype = n.appendChild(n.createElement("*")),
									s = {},
									l = function(e, t) {
										var n, o, r, i = e.childNodes || [],
											c = -1;
										if (1 === e.nodeType && e.constructor !== Element)
											for (n in e.constructor = Element, s) o = s[n], e[n] = o;
										for (; r = t && i[++c];) l(r, t);
										return e
									},
									u = document.getElementsByTagName("*"),
									o = document.createElement,
									r = 100;
								a.attachEvent("onpropertychange", function(e) {
									for (var t, n = e.propertyName, o = !s.hasOwnProperty(n), r = a[n], i = s[n], c = -1; t = u[++c];) 1 === t.nodeType && (o || t[n] === i) && (t[n] = r);
									s[n] = r
								}), a.constructor = Element, a.hasAttribute || (a.hasAttribute = function(e) {
									return null !== this.getAttribute(e)
								}), i() || (document.onreadystatechange = i, e = setInterval(i, 25)), document.createElement = function(e) {
									var t = o(String(e).toLowerCase());
									return l(t)
								}, document.removeChild(t)
							} else window.HTMLElement = window.Element;

							function i() {
								return r-- || clearTimeout(e), !(!document.body || document.body.prototype || !/(complete|interactive)/.test(document.readyState) || (l(document, !0), e && document.body.prototype && clearTimeout(e), !document.body.prototype))
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(i) {
						(function(e) {
							if (!("Event" in e)) return !1;
							if ("function" == typeof e.Event) return !0;
							try {
								return new Event("click"), !0
							} catch (t) {
								return !1
							}
						})(this) || function() {
							var n = {
								click: 1,
								dblclick: 1,
								keyup: 1,
								keypress: 1,
								keydown: 1,
								mousedown: 1,
								mouseup: 1,
								mousemove: 1,
								mouseover: 1,
								mouseenter: 1,
								mouseleave: 1,
								mouseout: 1,
								storage: 1,
								storagecommit: 1,
								textinput: 1
							};
							if ("undefined" != typeof document && "undefined" != typeof window) {
								var e = window.Event && window.Event.prototype || null;
								window.Event = Window.prototype.Event = function(e, t) {
									if (!e) throw new Error("Not enough arguments");
									var n;
									if ("createEvent" in document) {
										n = document.createEvent("Event");
										var o = !(!t || t.bubbles === i) && t.bubbles,
											r = !(!t || t.cancelable === i) && t.cancelable;
										return n.initEvent(e, o, r), n
									}
									return (n = document.createEventObject()).type = e, n.bubbles = !(!t || t.bubbles === i) && t.bubbles, n.cancelable = !(!t || t.cancelable === i) && t.cancelable, n
								}, e && Object.defineProperty(window.Event, "prototype", {
									configurable: !1,
									enumerable: !1,
									writable: !0,
									value: e
								}), "createEvent" in document || (window.addEventListener = Window.prototype.addEventListener = Document.prototype.addEventListener = Element.prototype.addEventListener = function() {
									var c = this,
										e = arguments[0],
										t = arguments[1];
									if (c === window && e in n) throw new Error("In IE8 the event: " + e + " is not available on the window object. Please see https://github.com/Financial-Times/polyfill-service/issues/317 for more information.");
									c._events || (c._events = {}), c._events[e] || (c._events[e] = function(e) {
										var t, n = c._events[e.type].list,
											o = n.slice(),
											r = -1,
											i = o.length;
										for (e.preventDefault = function() {
											!1 !== e.cancelable && (e.returnValue = !1)
										}, e.stopPropagation = function() {
											e.cancelBubble = !0
										}, e.stopImmediatePropagation = function() {
											e.cancelBubble = !0, e.cancelImmediate = !0
										}, e.currentTarget = c, e.relatedTarget = e.fromElement || null, e.target = e.target || e.srcElement || c, e.timeStamp = (new Date).getTime(), e.clientX && (e.pageX = e.clientX + document.documentElement.scrollLeft, e.pageY = e.clientY + document.documentElement.scrollTop); ++r < i && !e.cancelImmediate;) r in o && -1 !== a(n, t = o[r]) && "function" == typeof t && t.call(c, e)
									}, c._events[e].list = [], c.attachEvent && c.attachEvent("on" + e, c._events[e])), c._events[e].list.push(t)
								}, window.removeEventListener = Window.prototype.removeEventListener = Document.prototype.removeEventListener = Element.prototype.removeEventListener = function() {
									var e, t = this,
										n = arguments[0],
										o = arguments[1];
									t._events && t._events[n] && t._events[n].list && -1 !== (e = a(t._events[n].list, o)) && (t._events[n].list.splice(e, 1), t._events[n].list.length || (t.detachEvent && t.detachEvent("on" + n, t._events[n]), delete t._events[n]))
								}, window.dispatchEvent = Window.prototype.dispatchEvent = Document.prototype.dispatchEvent = Element.prototype.dispatchEvent = function(e) {
									if (!arguments.length) throw new Error("Not enough arguments");
									if (!e || "string" != typeof e.type) throw new Error("DOM Events Exception 0");
									var t = this,
										n = e.type;
									try {
										if (!e.bubbles) {
											e.cancelBubble = !0;
											var o = function(e) {
												e.cancelBubble = !0, (t || window).detachEvent("on" + n, o)
											};
											this.attachEvent("on" + n, o)
										}
										this.fireEvent("on" + n, e)
									} catch (r) {
										for (e.target = t;
											 "_events" in (e.currentTarget = t) && "function" == typeof t._events[n] && t._events[n].call(t, e), "function" == typeof t["on" + n] && t["on" + n].call(t, e), (t = 9 === t.nodeType ? t.parentWindow : t.parentNode) && !e.cancelBubble;);
									}
									return !0
								}, document.attachEvent("onreadystatechange", function() {
									"complete" === document.readyState && document.dispatchEvent(new Event("DOMContentLoaded", {
										bubbles: !0
									}))
								}))
							}

							function a(e, t) {
								for (var n = -1, o = e.length; ++n < o;)
									if (n in e && e[n] === t) return n;
								return -1
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(o) {
						var e, t, n;
						"DOMTokenList" in this && (!("classList" in (e = document.createElement("x"))) || !e.classList.toggle("x", !1) && !e.className) || ("DOMTokenList" in (t = this) && t.DOMTokenList && (!document.createElementNS || !document.createElementNS("http://www.w3.org/2000/svg", "svg") || document.createElementNS("http://www.w3.org/2000/svg", "svg").classList instanceof DOMTokenList) || (t.DOMTokenList = function() {
							var r = !0,
								n = function(e, t, n, o) {
									Object.defineProperty ? Object.defineProperty(e, t, {
										configurable: !1 === r || !!o,
										get: n
									}) : e.__defineGetter__(t, n)
								};
							try {
								n({}, "support")
							} catch (e) {
								r = !1
							}
							return function(r, i) {
								var c = this,
									a = [],
									s = {},
									l = 0,
									e = 0,
									t = function(e) {
										n(c, e, function() {
											return p(), a[e]
										}, !1)
									},
									u = function() {
										if (e <= l)
											for (; e < l; ++e) t(e)
									},
									p = function() {
										var e, t, n = arguments,
											o = /\s+/;
										if (n.length)
											for (t = 0; t < n.length; ++t)
												if (o.test(n[t])) throw (e = new SyntaxError('String "' + n[t] + '" contains an invalid character')).code = 5, e.name = "InvalidCharacterError", e;
										for ("" === (a = "object" == typeof r[i] ? ("" + r[i].baseVal).replace(/^\s+|\s+$/g, "").split(o) : ("" + r[i]).replace(/^\s+|\s+$/g, "").split(o))[0] && (a = []), s = {}, t = 0; t < a.length; ++t) s[a[t]] = !0;
										l = a.length, u()
									};
								return p(), n(c, "length", function() {
									return p(), l
								}), c.toLocaleString = c.toString = function() {
									return p(), a.join(" ")
								}, c.item = function(e) {
									return p(), a[e]
								}, c.contains = function(e) {
									return p(), !!s[e]
								}, c.add = function() {
									p.apply(c, e = arguments);
									for (var e, t, n = 0, o = e.length; n < o; ++n) t = e[n], s[t] || (a.push(t), s[t] = !0);
									l !== a.length && (l = a.length >>> 0, "object" == typeof r[i] ? r[i].baseVal = a.join(" ") : r[i] = a.join(" "), u())
								}, c.remove = function() {
									p.apply(c, e = arguments);
									for (var e, t = {}, n = 0, o = []; n < e.length; ++n) t[e[n]] = !0, delete s[e[n]];
									for (n = 0; n < a.length; ++n) t[a[n]] || o.push(a[n]);
									l = (a = o).length >>> 0, "object" == typeof r[i] ? r[i].baseVal = a.join(" ") : r[i] = a.join(" "), u()
								}, c.toggle = function(e, t) {
									return p.apply(c, [e]), o !== t ? t ? (c.add(e), !0) : (c.remove(e), !1) : s[e] ? (c.remove(e), !1) : (c.add(e), !0)
								}, c
							}
						}()), "classList" in (n = document.createElement("span")) && (n.classList.toggle("x", !1), n.classList.contains("x") && (n.classList.constructor.prototype.toggle = function(e) {
							var t = arguments[1];
							if (t !== o) return this[(t = !!t) ? "add" : "remove"](e), t;
							var n = !this.contains(e);
							return this[n ? "add" : "remove"](e), n
						})), function() {
							var e = document.createElement("span");
							if ("classList" in e && (e.classList.add("a", "b"), !e.classList.contains("b"))) {
								var o = e.classList.constructor.prototype.add;
								e.classList.constructor.prototype.add = function() {
									for (var e = arguments, t = arguments.length, n = 0; n < t; n++) o.call(this, e[n])
								}
							}
						}(), function() {
							var e = document.createElement("span");
							if ("classList" in e && (e.classList.add("a"), e.classList.add("b"), e.classList.remove("a", "b"), e.classList.contains("b"))) {
								var o = e.classList.constructor.prototype.remove;
								e.classList.constructor.prototype.remove = function() {
									for (var e = arguments, t = arguments.length, n = 0; n < t; n++) o.call(this, e[n])
								}
							}
						}())
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						var t;
						"document" in this && "classList" in document.documentElement && "Element" in this && "classList" in Element.prototype && ((t = document.createElement("span")).classList.add("a", "b"), t.classList.contains("b")) || function(e) {
							var u = !0,
								p = function(e, t, n, o) {
									Object.defineProperty ? Object.defineProperty(e, t, {
										configurable: !1 === u || !!o,
										get: n
									}) : e.__defineGetter__(t, n)
								};
							try {
								p({}, "support")
							} catch (t) {
								u = !1
							}
							var d = function(e, s, l) {
								p(e.prototype, s, function() {
									var e, t = this,
										n = "__defineGetter__DEFINE_PROPERTY" + s;
									if (t[n]) return e;
									if (!(t[n] = !0) === u) {
										for (var o, r = d.mirror || document.createElement("div"), i = r.childNodes, c = i.length, a = 0; a < c; ++a)
											if (i[a]._R === t) {
												o = i[a];
												break
											} o || (o = r.appendChild(document.createElement("div"))), e = DOMTokenList.call(o, t, l)
									} else e = new DOMTokenList(t, l);
									return p(t, s, function() {
										return e
									}), delete t[n], e
								}, !0)
							};
							d(e.Element, "classList", "className"), d(e.HTMLElement, "classList", "className"), d(e.HTMLLinkElement, "relList", "rel"), d(e.HTMLAnchorElement, "relList", "rel"), d(e.HTMLAreaElement, "relList", "rel")
						}(this)
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}), e.prototype.init = function() {
					var n = this.$module;
					t(n.querySelectorAll('input[type="radio"]'), function(e) {
						var t = e.getAttribute("data-aria-controls");
						t && n.querySelector("#" + t) && (e.setAttribute("aria-controls", t), e.removeAttribute("data-aria-controls"), this.setAttributes(e))
					}.bind(this)), n.addEventListener("click", this.handleClick.bind(this))
				}, e.prototype.setAttributes = function(e) {
					var t = document.querySelector("#" + e.getAttribute("aria-controls"));
					if (t && t.classList.contains("govuk-radios__conditional")) {
						var n = e.checked;
						e.setAttribute("aria-expanded", n), t.classList.toggle("govuk-radios__conditional--hidden", !n)
					}
				}, e.prototype.handleClick = function(e) {
					var n = e.target;
					"radio" === n.type && t(document.querySelectorAll('input[type="radio"][aria-controls]'), function(e) {
						var t = e.form === n.form;
						e.name === n.name && t && this.setAttributes(e)
					}.bind(this))
				}, e
			}()
		}),
		u = e(function(e, t) {
			e.exports = function() {
				function e(e) {
					this.$module = e
				}
				return function(e) {
					var a, s, l, u;
					"defineProperty" in Object && function() {
						try {
							return Object.defineProperty({}, "test", {
								value: 42
							}), !0
						} catch (e) {
							return !1
						}
					}() || (a = Object.defineProperty, s = Object.prototype.hasOwnProperty("__defineGetter__"), l = "Getters & setters cannot be defined on this javascript engine", u = "A property cannot both have accessors and be writable or have a value", Object.defineProperty = function(e, t, n) {
						if (a && (e === window || e === document || e === Element.prototype || e instanceof Element)) return a(e, t, n);
						if (null === e || !(e instanceof Object || "object" == typeof e)) throw new TypeError("Object.defineProperty called on non-object");
						if (!(n instanceof Object)) throw new TypeError("Property description must be an object");
						var o = String(t),
							r = "value" in n || "writable" in n,
							i = "get" in n && typeof n.get,
							c = "set" in n && typeof n.set;
						if (i) {
							if ("function" !== i) throw new TypeError("Getter must be a function");
							if (!s) throw new TypeError(l);
							if (r) throw new TypeError(u);
							Object.__defineGetter__.call(e, o, n.get)
						} else e[o] = n.value;
						if (c) {
							if ("function" !== c) throw new TypeError("Setter must be a function");
							if (!s) throw new TypeError(l);
							if (r) throw new TypeError(u);
							Object.__defineSetter__.call(e, o, n.set)
						}
						return "value" in n && (e[o] = n.value), e
					})
				}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"bind" in Function.prototype || Object.defineProperty(Function.prototype, "bind", {
							value: function(t) {
								var n, e = Array,
									o = Object,
									r = o.prototype,
									i = e.prototype,
									c = function c() {},
									a = r.toString,
									s = "function" == typeof Symbol && "symbol" == typeof Symbol.toStringTag,
									l = Function.prototype.toString,
									u = function u(e) {
										try {
											return l.call(e), !0
										} catch (t) {
											return !1
										}
									};
								n = function n(e) {
									if ("function" != typeof e) return !1;
									if (s) return u(e);
									var t = a.call(e);
									return "[object Function]" === t || "[object GeneratorFunction]" === t
								};
								var p = i.slice,
									d = i.concat,
									f = i.push,
									m = Math.max,
									y = this;
								if (!n(y)) throw new TypeError("Function.prototype.bind called on incompatible " + y);
								for (var h, b = p.call(arguments, 1), v = m(0, y.length - b.length), w = [], E = 0; E < v; E++) f.call(w, "$" + E);
								return h = Function("binder", "return function (" + w.join(",") + "){ return binder.apply(this, arguments); }")(function() {
									if (this instanceof h) {
										var e = y.apply(this, d.call(b, p.call(arguments)));
										return o(e) === e ? e : this
									}
									return y.apply(t, d.call(b, p.call(arguments)))
								}), y.prototype && (c.prototype = y.prototype, h.prototype = new c, c.prototype = null), h
							}
						})
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Window" in this || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && function(e) {
							e.constructor ? e.Window = e.constructor : (e.Window = e.constructor = new Function("return function Window() {}")()).prototype = this
						}(this)
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Document" in this || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && (this.HTMLDocument ? this.Document = this.HTMLDocument : (this.Document = this.HTMLDocument = document.constructor = new Function("return function Document() {}")(), this.Document.prototype = document))
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Element" in this && "HTMLElement" in this || function() {
							if (!window.Element || window.HTMLElement) {
								window.Element = window.HTMLElement = new Function("return function Element() {}")();
								var e, t = document.appendChild(document.createElement("body")),
									n = t.appendChild(document.createElement("iframe")).contentWindow.document,
									a = Element.prototype = n.appendChild(n.createElement("*")),
									s = {},
									l = function(e, t) {
										var n, o, r, i = e.childNodes || [],
											c = -1;
										if (1 === e.nodeType && e.constructor !== Element)
											for (n in e.constructor = Element, s) o = s[n], e[n] = o;
										for (; r = t && i[++c];) l(r, t);
										return e
									},
									u = document.getElementsByTagName("*"),
									o = document.createElement,
									r = 100;
								a.attachEvent("onpropertychange", function(e) {
									for (var t, n = e.propertyName, o = !s.hasOwnProperty(n), r = a[n], i = s[n], c = -1; t = u[++c];) 1 === t.nodeType && (o || t[n] === i) && (t[n] = r);
									s[n] = r
								}), a.constructor = Element, a.hasAttribute || (a.hasAttribute = function(e) {
									return null !== this.getAttribute(e)
								}), i() || (document.onreadystatechange = i, e = setInterval(i, 25)), document.createElement = function(e) {
									var t = o(String(e).toLowerCase());
									return l(t)
								}, document.removeChild(t)
							} else window.HTMLElement = window.Element;

							function i() {
								return r-- || clearTimeout(e), !(!document.body || document.body.prototype || !/(complete|interactive)/.test(document.readyState) || (l(document, !0), e && document.body.prototype && clearTimeout(e), !document.body.prototype))
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(i) {
						(function(e) {
							if (!("Event" in e)) return !1;
							if ("function" == typeof e.Event) return !0;
							try {
								return new Event("click"), !0
							} catch (t) {
								return !1
							}
						})(this) || function() {
							var n = {
								click: 1,
								dblclick: 1,
								keyup: 1,
								keypress: 1,
								keydown: 1,
								mousedown: 1,
								mouseup: 1,
								mousemove: 1,
								mouseover: 1,
								mouseenter: 1,
								mouseleave: 1,
								mouseout: 1,
								storage: 1,
								storagecommit: 1,
								textinput: 1
							};
							if ("undefined" != typeof document && "undefined" != typeof window) {
								var e = window.Event && window.Event.prototype || null;
								window.Event = Window.prototype.Event = function(e, t) {
									if (!e) throw new Error("Not enough arguments");
									var n;
									if ("createEvent" in document) {
										n = document.createEvent("Event");
										var o = !(!t || t.bubbles === i) && t.bubbles,
											r = !(!t || t.cancelable === i) && t.cancelable;
										return n.initEvent(e, o, r), n
									}
									return (n = document.createEventObject()).type = e, n.bubbles = !(!t || t.bubbles === i) && t.bubbles, n.cancelable = !(!t || t.cancelable === i) && t.cancelable, n
								}, e && Object.defineProperty(window.Event, "prototype", {
									configurable: !1,
									enumerable: !1,
									writable: !0,
									value: e
								}), "createEvent" in document || (window.addEventListener = Window.prototype.addEventListener = Document.prototype.addEventListener = Element.prototype.addEventListener = function() {
									var c = this,
										e = arguments[0],
										t = arguments[1];
									if (c === window && e in n) throw new Error("In IE8 the event: " + e + " is not available on the window object. Please see https://github.com/Financial-Times/polyfill-service/issues/317 for more information.");
									c._events || (c._events = {}), c._events[e] || (c._events[e] = function(e) {
										var t, n = c._events[e.type].list,
											o = n.slice(),
											r = -1,
											i = o.length;
										for (e.preventDefault = function() {
											!1 !== e.cancelable && (e.returnValue = !1)
										}, e.stopPropagation = function() {
											e.cancelBubble = !0
										}, e.stopImmediatePropagation = function() {
											e.cancelBubble = !0, e.cancelImmediate = !0
										}, e.currentTarget = c, e.relatedTarget = e.fromElement || null, e.target = e.target || e.srcElement || c, e.timeStamp = (new Date).getTime(), e.clientX && (e.pageX = e.clientX + document.documentElement.scrollLeft, e.pageY = e.clientY + document.documentElement.scrollTop); ++r < i && !e.cancelImmediate;) r in o && -1 !== a(n, t = o[r]) && "function" == typeof t && t.call(c, e)
									}, c._events[e].list = [], c.attachEvent && c.attachEvent("on" + e, c._events[e])), c._events[e].list.push(t)
								}, window.removeEventListener = Window.prototype.removeEventListener = Document.prototype.removeEventListener = Element.prototype.removeEventListener = function() {
									var e, t = this,
										n = arguments[0],
										o = arguments[1];
									t._events && t._events[n] && t._events[n].list && -1 !== (e = a(t._events[n].list, o)) && (t._events[n].list.splice(e, 1), t._events[n].list.length || (t.detachEvent && t.detachEvent("on" + n, t._events[n]), delete t._events[n]))
								}, window.dispatchEvent = Window.prototype.dispatchEvent = Document.prototype.dispatchEvent = Element.prototype.dispatchEvent = function(e) {
									if (!arguments.length) throw new Error("Not enough arguments");
									if (!e || "string" != typeof e.type) throw new Error("DOM Events Exception 0");
									var t = this,
										n = e.type;
									try {
										if (!e.bubbles) {
											e.cancelBubble = !0;
											var o = function(e) {
												e.cancelBubble = !0, (t || window).detachEvent("on" + n, o)
											};
											this.attachEvent("on" + n, o)
										}
										this.fireEvent("on" + n, e)
									} catch (r) {
										for (e.target = t;
											 "_events" in (e.currentTarget = t) && "function" == typeof t._events[n] && t._events[n].call(t, e), "function" == typeof t["on" + n] && t["on" + n].call(t, e), (t = 9 === t.nodeType ? t.parentWindow : t.parentNode) && !e.cancelBubble;);
									}
									return !0
								}, document.attachEvent("onreadystatechange", function() {
									"complete" === document.readyState && document.dispatchEvent(new Event("DOMContentLoaded", {
										bubbles: !0
									}))
								}))
							}

							function a(e, t) {
								for (var n = -1, o = e.length; ++n < o;)
									if (n in e && e[n] === t) return n;
								return -1
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}), e.prototype.init = function() {
					var e = this.$module;
					if (e) {
						var t = e.querySelector(".govuk-js-header-toggle");
						t && t.addEventListener("click", this.handleClick.bind(this))
					}
				}, e.prototype.toggleClass = function(e, t) {
					0 < e.className.indexOf(t) ? e.className = e.className.replace(" " + t, "") : e.className += " " + t
				}, e.prototype.handleClick = function(e) {
					var t = this.$module,
						n = e.target || e.srcElement,
						o = t.querySelector("#" + n.getAttribute("aria-controls"));
					n && o && (this.toggleClass(o, "govuk-header__navigation--open"), this.toggleClass(n, "govuk-header__menu-button--open"), n.setAttribute("aria-expanded", "true" !== n.getAttribute("aria-expanded")), o.setAttribute("aria-hidden", "false" === o.getAttribute("aria-hidden")))
				}, e
			}()
		}),
		p = e(function(e, t) {
			e.exports = function() {
				function i(e, t) {
					if (window.NodeList.prototype.forEach) return e.forEach(t);
					for (var n = 0; n < e.length; n++) t.call(window, e[n], n, e)
				}

				function e(e) {
					this.$module = e, this.$tabs = e.querySelectorAll(".govuk-tabs__tab"), this.keys = {
						left: 37,
						right: 39,
						up: 38,
						down: 40
					}, this.jsHiddenClass = "govuk-tabs__panel--hidden"
				}
				return function(e) {
					var a, s, l, u;
					"defineProperty" in Object && function() {
						try {
							return Object.defineProperty({}, "test", {
								value: 42
							}), !0
						} catch (e) {
							return !1
						}
					}() || (a = Object.defineProperty, s = Object.prototype.hasOwnProperty("__defineGetter__"), l = "Getters & setters cannot be defined on this javascript engine", u = "A property cannot both have accessors and be writable or have a value", Object.defineProperty = function(e, t, n) {
						if (a && (e === window || e === document || e === Element.prototype || e instanceof Element)) return a(e, t, n);
						if (null === e || !(e instanceof Object || "object" == typeof e)) throw new TypeError("Object.defineProperty called on non-object");
						if (!(n instanceof Object)) throw new TypeError("Property description must be an object");
						var o = String(t),
							r = "value" in n || "writable" in n,
							i = "get" in n && typeof n.get,
							c = "set" in n && typeof n.set;
						if (i) {
							if ("function" !== i) throw new TypeError("Getter must be a function");
							if (!s) throw new TypeError(l);
							if (r) throw new TypeError(u);
							Object.__defineGetter__.call(e, o, n.get)
						} else e[o] = n.value;
						if (c) {
							if ("function" !== c) throw new TypeError("Setter must be a function");
							if (!s) throw new TypeError(l);
							if (r) throw new TypeError(u);
							Object.__defineSetter__.call(e, o, n.set)
						}
						return "value" in n && (e[o] = n.value), e
					})
				}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"bind" in Function.prototype || Object.defineProperty(Function.prototype, "bind", {
							value: function(t) {
								var n, e = Array,
									o = Object,
									r = o.prototype,
									i = e.prototype,
									c = function c() {},
									a = r.toString,
									s = "function" == typeof Symbol && "symbol" == typeof Symbol.toStringTag,
									l = Function.prototype.toString,
									u = function u(e) {
										try {
											return l.call(e), !0
										} catch (t) {
											return !1
										}
									};
								n = function n(e) {
									if ("function" != typeof e) return !1;
									if (s) return u(e);
									var t = a.call(e);
									return "[object Function]" === t || "[object GeneratorFunction]" === t
								};
								var p = i.slice,
									d = i.concat,
									f = i.push,
									m = Math.max,
									y = this;
								if (!n(y)) throw new TypeError("Function.prototype.bind called on incompatible " + y);
								for (var h, b = p.call(arguments, 1), v = m(0, y.length - b.length), w = [], E = 0; E < v; E++) f.call(w, "$" + E);
								return h = Function("binder", "return function (" + w.join(",") + "){ return binder.apply(this, arguments); }")(function() {
									if (this instanceof h) {
										var e = y.apply(this, d.call(b, p.call(arguments)));
										return o(e) === e ? e : this
									}
									return y.apply(t, d.call(b, p.call(arguments)))
								}), y.prototype && (c.prototype = y.prototype, h.prototype = new c, c.prototype = null), h
							}
						})
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(o) {
						var e, t, n;
						"DOMTokenList" in this && (!("classList" in (e = document.createElement("x"))) || !e.classList.toggle("x", !1) && !e.className) || ("DOMTokenList" in (t = this) && t.DOMTokenList && (!document.createElementNS || !document.createElementNS("http://www.w3.org/2000/svg", "svg") || document.createElementNS("http://www.w3.org/2000/svg", "svg").classList instanceof DOMTokenList) || (t.DOMTokenList = function() {
							var r = !0,
								n = function(e, t, n, o) {
									Object.defineProperty ? Object.defineProperty(e, t, {
										configurable: !1 === r || !!o,
										get: n
									}) : e.__defineGetter__(t, n)
								};
							try {
								n({}, "support")
							} catch (e) {
								r = !1
							}
							return function(r, i) {
								var c = this,
									a = [],
									s = {},
									l = 0,
									e = 0,
									t = function(e) {
										n(c, e, function() {
											return p(), a[e]
										}, !1)
									},
									u = function() {
										if (e <= l)
											for (; e < l; ++e) t(e)
									},
									p = function() {
										var e, t, n = arguments,
											o = /\s+/;
										if (n.length)
											for (t = 0; t < n.length; ++t)
												if (o.test(n[t])) throw (e = new SyntaxError('String "' + n[t] + '" contains an invalid character')).code = 5, e.name = "InvalidCharacterError", e;
										for ("" === (a = "object" == typeof r[i] ? ("" + r[i].baseVal).replace(/^\s+|\s+$/g, "").split(o) : ("" + r[i]).replace(/^\s+|\s+$/g, "").split(o))[0] && (a = []), s = {}, t = 0; t < a.length; ++t) s[a[t]] = !0;
										l = a.length, u()
									};
								return p(), n(c, "length", function() {
									return p(), l
								}), c.toLocaleString = c.toString = function() {
									return p(), a.join(" ")
								}, c.item = function(e) {
									return p(), a[e]
								}, c.contains = function(e) {
									return p(), !!s[e]
								}, c.add = function() {
									p.apply(c, e = arguments);
									for (var e, t, n = 0, o = e.length; n < o; ++n) t = e[n], s[t] || (a.push(t), s[t] = !0);
									l !== a.length && (l = a.length >>> 0, "object" == typeof r[i] ? r[i].baseVal = a.join(" ") : r[i] = a.join(" "), u())
								}, c.remove = function() {
									p.apply(c, e = arguments);
									for (var e, t = {}, n = 0, o = []; n < e.length; ++n) t[e[n]] = !0, delete s[e[n]];
									for (n = 0; n < a.length; ++n) t[a[n]] || o.push(a[n]);
									l = (a = o).length >>> 0, "object" == typeof r[i] ? r[i].baseVal = a.join(" ") : r[i] = a.join(" "), u()
								}, c.toggle = function(e, t) {
									return p.apply(c, [e]), o !== t ? t ? (c.add(e), !0) : (c.remove(e), !1) : s[e] ? (c.remove(e), !1) : (c.add(e), !0)
								}, c
							}
						}()), "classList" in (n = document.createElement("span")) && (n.classList.toggle("x", !1), n.classList.contains("x") && (n.classList.constructor.prototype.toggle = function(e) {
							var t = arguments[1];
							if (t !== o) return this[(t = !!t) ? "add" : "remove"](e), t;
							var n = !this.contains(e);
							return this[n ? "add" : "remove"](e), n
						})), function() {
							var e = document.createElement("span");
							if ("classList" in e && (e.classList.add("a", "b"), !e.classList.contains("b"))) {
								var o = e.classList.constructor.prototype.add;
								e.classList.constructor.prototype.add = function() {
									for (var e = arguments, t = arguments.length, n = 0; n < t; n++) o.call(this, e[n])
								}
							}
						}(), function() {
							var e = document.createElement("span");
							if ("classList" in e && (e.classList.add("a"), e.classList.add("b"), e.classList.remove("a", "b"), e.classList.contains("b"))) {
								var o = e.classList.constructor.prototype.remove;
								e.classList.constructor.prototype.remove = function() {
									for (var e = arguments, t = arguments.length, n = 0; n < t; n++) o.call(this, e[n])
								}
							}
						}())
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Document" in this || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && (this.HTMLDocument ? this.Document = this.HTMLDocument : (this.Document = this.HTMLDocument = document.constructor = new Function("return function Document() {}")(), this.Document.prototype = document))
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Element" in this && "HTMLElement" in this || function() {
							if (!window.Element || window.HTMLElement) {
								window.Element = window.HTMLElement = new Function("return function Element() {}")();
								var e, t = document.appendChild(document.createElement("body")),
									n = t.appendChild(document.createElement("iframe")).contentWindow.document,
									a = Element.prototype = n.appendChild(n.createElement("*")),
									s = {},
									l = function(e, t) {
										var n, o, r, i = e.childNodes || [],
											c = -1;
										if (1 === e.nodeType && e.constructor !== Element)
											for (n in e.constructor = Element, s) o = s[n], e[n] = o;
										for (; r = t && i[++c];) l(r, t);
										return e
									},
									u = document.getElementsByTagName("*"),
									o = document.createElement,
									r = 100;
								a.attachEvent("onpropertychange", function(e) {
									for (var t, n = e.propertyName, o = !s.hasOwnProperty(n), r = a[n], i = s[n], c = -1; t = u[++c];) 1 === t.nodeType && (o || t[n] === i) && (t[n] = r);
									s[n] = r
								}), a.constructor = Element, a.hasAttribute || (a.hasAttribute = function(e) {
									return null !== this.getAttribute(e)
								}), i() || (document.onreadystatechange = i, e = setInterval(i, 25)), document.createElement = function(e) {
									var t = o(String(e).toLowerCase());
									return l(t)
								}, document.removeChild(t)
							} else window.HTMLElement = window.Element;

							function i() {
								return r-- || clearTimeout(e), !(!document.body || document.body.prototype || !/(complete|interactive)/.test(document.readyState) || (l(document, !0), e && document.body.prototype && clearTimeout(e), !document.body.prototype))
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						var t;
						"document" in this && "classList" in document.documentElement && "Element" in this && "classList" in Element.prototype && ((t = document.createElement("span")).classList.add("a", "b"), t.classList.contains("b")) || function(e) {
							var u = !0,
								p = function(e, t, n, o) {
									Object.defineProperty ? Object.defineProperty(e, t, {
										configurable: !1 === u || !!o,
										get: n
									}) : e.__defineGetter__(t, n)
								};
							try {
								p({}, "support")
							} catch (t) {
								u = !1
							}
							var d = function(e, s, l) {
								p(e.prototype, s, function() {
									var e, t = this,
										n = "__defineGetter__DEFINE_PROPERTY" + s;
									if (t[n]) return e;
									if (!(t[n] = !0) === u) {
										for (var o, r = d.mirror || document.createElement("div"), i = r.childNodes, c = i.length, a = 0; a < c; ++a)
											if (i[a]._R === t) {
												o = i[a];
												break
											} o || (o = r.appendChild(document.createElement("div"))), e = DOMTokenList.call(o, t, l)
									} else e = new DOMTokenList(t, l);
									return p(t, s, function() {
										return e
									}), delete t[n], e
								}, !0)
							};
							d(e.Element, "classList", "className"), d(e.HTMLElement, "classList", "className"), d(e.HTMLLinkElement, "relList", "rel"), d(e.HTMLAnchorElement, "relList", "rel"), d(e.HTMLAreaElement, "relList", "rel")
						}(this)
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Element" in this && "nextElementSibling" in document.documentElement || Object.defineProperty(Element.prototype, "nextElementSibling", {
							get: function() {
								for (var e = this.nextSibling; e && 1 !== e.nodeType;) e = e.nextSibling;
								return 1 === e.nodeType ? e : null
							}
						})
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Element" in this && "previousElementSibling" in document.documentElement || Object.defineProperty(Element.prototype, "previousElementSibling", {
							get: function() {
								for (var e = this.previousSibling; e && 1 !== e.nodeType;) e = e.previousSibling;
								return 1 === e.nodeType ? e : null
							}
						})
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(e) {
						"Window" in this || "undefined" == typeof WorkerGlobalScope && "function" != typeof importScripts && function(e) {
							e.constructor ? e.Window = e.constructor : (e.Window = e.constructor = new Function("return function Window() {}")()).prototype = this
						}(this)
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}),
					function(i) {
						(function(e) {
							if (!("Event" in e)) return !1;
							if ("function" == typeof e.Event) return !0;
							try {
								return new Event("click"), !0
							} catch (t) {
								return !1
							}
						})(this) || function() {
							var n = {
								click: 1,
								dblclick: 1,
								keyup: 1,
								keypress: 1,
								keydown: 1,
								mousedown: 1,
								mouseup: 1,
								mousemove: 1,
								mouseover: 1,
								mouseenter: 1,
								mouseleave: 1,
								mouseout: 1,
								storage: 1,
								storagecommit: 1,
								textinput: 1
							};
							if ("undefined" != typeof document && "undefined" != typeof window) {
								var e = window.Event && window.Event.prototype || null;
								window.Event = Window.prototype.Event = function(e, t) {
									if (!e) throw new Error("Not enough arguments");
									var n;
									if ("createEvent" in document) {
										n = document.createEvent("Event");
										var o = !(!t || t.bubbles === i) && t.bubbles,
											r = !(!t || t.cancelable === i) && t.cancelable;
										return n.initEvent(e, o, r), n
									}
									return (n = document.createEventObject()).type = e, n.bubbles = !(!t || t.bubbles === i) && t.bubbles, n.cancelable = !(!t || t.cancelable === i) && t.cancelable, n
								}, e && Object.defineProperty(window.Event, "prototype", {
									configurable: !1,
									enumerable: !1,
									writable: !0,
									value: e
								}), "createEvent" in document || (window.addEventListener = Window.prototype.addEventListener = Document.prototype.addEventListener = Element.prototype.addEventListener = function() {
									var c = this,
										e = arguments[0],
										t = arguments[1];
									if (c === window && e in n) throw new Error("In IE8 the event: " + e + " is not available on the window object. Please see https://github.com/Financial-Times/polyfill-service/issues/317 for more information.");
									c._events || (c._events = {}), c._events[e] || (c._events[e] = function(e) {
										var t, n = c._events[e.type].list,
											o = n.slice(),
											r = -1,
											i = o.length;
										for (e.preventDefault = function() {
											!1 !== e.cancelable && (e.returnValue = !1)
										}, e.stopPropagation = function() {
											e.cancelBubble = !0
										}, e.stopImmediatePropagation = function() {
											e.cancelBubble = !0, e.cancelImmediate = !0
										}, e.currentTarget = c, e.relatedTarget = e.fromElement || null, e.target = e.target || e.srcElement || c, e.timeStamp = (new Date).getTime(), e.clientX && (e.pageX = e.clientX + document.documentElement.scrollLeft, e.pageY = e.clientY + document.documentElement.scrollTop); ++r < i && !e.cancelImmediate;) r in o && -1 !== a(n, t = o[r]) && "function" == typeof t && t.call(c, e)
									}, c._events[e].list = [], c.attachEvent && c.attachEvent("on" + e, c._events[e])), c._events[e].list.push(t)
								}, window.removeEventListener = Window.prototype.removeEventListener = Document.prototype.removeEventListener = Element.prototype.removeEventListener = function() {
									var e, t = this,
										n = arguments[0],
										o = arguments[1];
									t._events && t._events[n] && t._events[n].list && -1 !== (e = a(t._events[n].list, o)) && (t._events[n].list.splice(e, 1), t._events[n].list.length || (t.detachEvent && t.detachEvent("on" + n, t._events[n]), delete t._events[n]))
								}, window.dispatchEvent = Window.prototype.dispatchEvent = Document.prototype.dispatchEvent = Element.prototype.dispatchEvent = function(e) {
									if (!arguments.length) throw new Error("Not enough arguments");
									if (!e || "string" != typeof e.type) throw new Error("DOM Events Exception 0");
									var t = this,
										n = e.type;
									try {
										if (!e.bubbles) {
											e.cancelBubble = !0;
											var o = function(e) {
												e.cancelBubble = !0, (t || window).detachEvent("on" + n, o)
											};
											this.attachEvent("on" + n, o)
										}
										this.fireEvent("on" + n, e)
									} catch (r) {
										for (e.target = t;
											 "_events" in (e.currentTarget = t) && "function" == typeof t._events[n] && t._events[n].call(t, e), "function" == typeof t["on" + n] && t["on" + n].call(t, e), (t = 9 === t.nodeType ? t.parentWindow : t.parentNode) && !e.cancelBubble;);
									}
									return !0
								}, document.attachEvent("onreadystatechange", function() {
									"complete" === document.readyState && document.dispatchEvent(new Event("DOMContentLoaded", {
										bubbles: !0
									}))
								}))
							}

							function a(e, t) {
								for (var n = -1, o = e.length; ++n < o;)
									if (n in e && e[n] === t) return n;
								return -1
							}
						}()
					}.call("object" == typeof window && window || "object" == typeof self && self || "object" == typeof n && n || {}), e.prototype.init = function() {
					"function" == typeof window.matchMedia ? this.setupResponsiveChecks() : this.setup()
				}, e.prototype.setupResponsiveChecks = function() {
					this.mql = window.matchMedia("(min-width: 40.0625em)"), this.mql.addListener(this.checkMode.bind(this)), this.checkMode()
				}, e.prototype.checkMode = function() {
					this.mql.matches ? this.setup() : this.teardown()
				}, e.prototype.setup = function() {
					var e = this.$module,
						t = this.$tabs,
						n = e.querySelector(".govuk-tabs__list"),
						o = e.querySelectorAll(".govuk-tabs__list-item");
					if (t && n && o) {
						n.setAttribute("role", "tablist"), i(o, function(e) {
							e.setAttribute("role", "presentation")
						}), i(t, function(e) {
							this.setAttributes(e), e.boundTabClick = this.onTabClick.bind(this), e.boundTabKeydown = this.onTabKeydown.bind(this), e.addEventListener("click", e.boundTabClick, !0), e.addEventListener("keydown", e.boundTabKeydown, !0), this.hideTab(e)
						}.bind(this));
						var r = this.getTab(window.location.hash) || this.$tabs[0];
						this.showTab(r), e.boundOnHashChange = this.onHashChange.bind(this), window.addEventListener("hashchange", e.boundOnHashChange, !0)
					}
				}, e.prototype.teardown = function() {
					var e = this.$module,
						t = this.$tabs,
						n = e.querySelector(".govuk-tabs__list"),
						o = e.querySelectorAll(".govuk-tabs__list-item");
					t && n && o && (n.removeAttribute("role"), i(o, function(e) {
						e.removeAttribute("role", "presentation")
					}), i(t, function(e) {
						e.removeEventListener("click", e.boundTabClick, !0), e.removeEventListener("keydown", e.boundTabKeydown, !0), this.unsetAttributes(e)
					}.bind(this)), window.removeEventListener("hashchange", e.boundOnHashChange, !0))
				}, e.prototype.onHashChange = function(e) {
					var t = window.location.hash,
						n = this.getTab(t);
					if (n)
						if (this.changingHash) this.changingHash = !1;
						else {
							var o = this.getCurrentTab();
							this.hideTab(o), this.showTab(n), n.focus()
						}
				}, e.prototype.hideTab = function(e) {
					this.unhighlightTab(e), this.hidePanel(e)
				}, e.prototype.showTab = function(e) {
					this.highlightTab(e), this.showPanel(e)
				}, e.prototype.getTab = function(e) {
					return this.$module.querySelector('.govuk-tabs__tab[href="' + e + '"]')
				}, e.prototype.setAttributes = function(e) {
					var t = this.getHref(e).slice(1);
					e.setAttribute("id", "tab_" + t), e.setAttribute("role", "tab"), e.setAttribute("aria-controls", t), e.setAttribute("aria-selected", "false"), e.setAttribute("tabindex", "-1");
					var n = this.getPanel(e);
					n.setAttribute("role", "tabpanel"), n.setAttribute("aria-labelledby", e.id), n.classList.add(this.jsHiddenClass)
				}, e.prototype.unsetAttributes = function(e) {
					e.removeAttribute("id"), e.removeAttribute("role"), e.removeAttribute("aria-controls"), e.removeAttribute("aria-selected"), e.removeAttribute("tabindex");
					var t = this.getPanel(e);
					t.removeAttribute("role"), t.removeAttribute("aria-labelledby"), t.classList.remove(this.jsHiddenClass)
				}, e.prototype.onTabClick = function(e) {
					if (!e.target.classList.contains("govuk-tabs__tab")) return !1;
					e.preventDefault();
					var t = e.target,
						n = this.getCurrentTab();
					this.hideTab(n), this.showTab(t), this.createHistoryEntry(t)
				}, e.prototype.createHistoryEntry = function(e) {
					var t = this.getPanel(e),
						n = t.id;
					t.id = "", this.changingHash = !0, window.location.hash = this.getHref(e).slice(1), t.id = n
				}, e.prototype.onTabKeydown = function(e) {
					switch (e.keyCode) {
						case this.keys.left:
						case this.keys.up:
							this.activatePreviousTab(), e.preventDefault();
							break;
						case this.keys.right:
						case this.keys.down:
							this.activateNextTab(), e.preventDefault()
					}
				}, e.prototype.activateNextTab = function() {
					var e = this.getCurrentTab(),
						t = e.parentNode.nextElementSibling;
					if (t) var n = t.querySelector(".govuk-tabs__tab");
					n && (this.hideTab(e), this.showTab(n), n.focus(), this.createHistoryEntry(n))
				}, e.prototype.activatePreviousTab = function() {
					var e = this.getCurrentTab(),
						t = e.parentNode.previousElementSibling;
					if (t) var n = t.querySelector(".govuk-tabs__tab");
					n && (this.hideTab(e), this.showTab(n), n.focus(), this.createHistoryEntry(n))
				}, e.prototype.getPanel = function(e) {
					return this.$module.querySelector(this.getHref(e))
				}, e.prototype.showPanel = function(e) {
					this.getPanel(e).classList.remove(this.jsHiddenClass)
				}, e.prototype.hidePanel = function(e) {
					this.getPanel(e).classList.add(this.jsHiddenClass)
				}, e.prototype.unhighlightTab = function(e) {
					e.setAttribute("aria-selected", "false"), e.parentNode.classList.remove("govuk-tabs__list-item--selected"), e.setAttribute("tabindex", "-1")
				}, e.prototype.highlightTab = function(e) {
					e.setAttribute("aria-selected", "true"), e.parentNode.classList.add("govuk-tabs__list-item--selected"), e.setAttribute("tabindex", "0")
				}, e.prototype.getCurrentTab = function() {
					return this.$module.querySelector(".govuk-tabs__list-item--selected .govuk-tabs__tab")
				}, e.prototype.getHref = function(e) {
					var t = e.getAttribute("href");
					return t.slice(t.indexOf("#"), t.length)
				}, e
			}()
		}),
		d = t.nodeListForEach;
	d(document.querySelectorAll('[data-module="govuk-button"]'), function(e) {
		new r(e).init()
	}), d(document.querySelectorAll('[data-module="govuk-accordion"]'), function(e) {
		new o(e).init()
	}), d(document.querySelectorAll('[data-module="govuk-details"]'), function(e) {
		new i(e).init()
	}), d(document.querySelectorAll('[data-module="govuk-error-summary"]'), function(e) {
		var t = new s(e);
		t.init = function() {
			this.$module.addEventListener("click", s.prototype.handleClick.bind(this))
		}, t.init()
	}), d(document.querySelectorAll('[data-module="govuk-character-count"]'), function(e) {
		new c(e).init()
	}), d(document.querySelectorAll('[data-module="govuk-checkboxes"]'), function(e) {
		new a(e).init()
	}), d(document.querySelectorAll('[data-module="govuk-radios"]'), function(e) {
		new l(e).init()
	}), d(document.querySelectorAll('[data-module="govuk-header"]'), function(e) {
		new u(e).init()
	}), d(document.querySelectorAll('[data-module="govuk-tabs"]'), function(e) {
		new p(e).init()
	})
}();
//# sourceMappingURL=govuk-frontend.js.map
