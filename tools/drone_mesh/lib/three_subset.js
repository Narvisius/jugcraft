// A small stand-in for three.js r128, holding only what drones.js and export.html use: the maths classes, the scene
// graph, BufferGeometry and the nine geometries the drones are built from (box, cylinder, sphere, torus, lathe, plane,
// circle, ring, extrude with Shape/Path and earcut), materials and canvas textures. It follows r128's algorithms
// (vertex order, winding, UVs, Float32 positions) so export.py produces the same meshes as with the real library.
// export.py serves it in place of lib/three.min.js when that file is missing (the export machine may be offline).
// three.js is MIT licensed (c) 2010-2021 three.js authors; earcut is ISC licensed (c) 2016 Mapbox.
(function (global) {
'use strict';
const THREE = { REVISION: '128-subset' };

// ---------------------------------------------------------------- constants
Object.assign(THREE, {
	FrontSide: 0, BackSide: 1, DoubleSide: 2, FlatShading: 1, SmoothShading: 2,
	NoBlending: 0, NormalBlending: 1, AdditiveBlending: 2, SubtractiveBlending: 3, MultiplyBlending: 4, CustomBlending: 5,
	RepeatWrapping: 1000, ClampToEdgeWrapping: 1001, MirroredRepeatWrapping: 1002,
	NearestFilter: 1003, NearestMipmapNearestFilter: 1004, NearestMipmapLinearFilter: 1005,
	LinearFilter: 1006, LinearMipmapNearestFilter: 1007, LinearMipmapLinearFilter: 1008,
	UVMapping: 300, RGBAFormat: 1023, UnsignedByteType: 1009,
	LinearEncoding: 3000, sRGBEncoding: 3001,
	TrianglesDrawMode: 0, TriangleStripDrawMode: 1, TriangleFanDrawMode: 2,
	StaticDrawUsage: 35044, DynamicDrawUsage: 35048,
});

// ---------------------------------------------------------------- MathUtils
const _lut = [];
for (let i = 0; i < 256; i++) _lut[i] = (i < 16 ? '0' : '') + i.toString(16);
function generateUUID() {
	const d0 = Math.random() * 0xffffffff | 0, d1 = Math.random() * 0xffffffff | 0, d2 = Math.random() * 0xffffffff | 0, d3 = Math.random() * 0xffffffff | 0;
	const uuid = _lut[d0 & 0xff] + _lut[d0 >> 8 & 0xff] + _lut[d0 >> 16 & 0xff] + _lut[d0 >> 24 & 0xff] + '-' + _lut[d1 & 0xff] + _lut[d1 >> 8 & 0xff] + '-'
		+ _lut[d1 >> 16 & 0x0f | 0x40] + _lut[d1 >> 24 & 0xff] + '-' + _lut[d2 & 0x3f | 0x80] + _lut[d2 >> 8 & 0xff] + '-' + _lut[d2 >> 16 & 0xff]
		+ _lut[d2 >> 24 & 0xff] + _lut[d3 & 0xff] + _lut[d3 >> 8 & 0xff] + _lut[d3 >> 16 & 0xff] + _lut[d3 >> 24 & 0xff];
	return uuid.toUpperCase();
}
function clamp(value, min, max) { return Math.max(min, Math.min(max, value)); }
const MathUtils = {
	DEG2RAD: Math.PI / 180, RAD2DEG: 180 / Math.PI, generateUUID, clamp,
	lerp: (x, y, t) => (1 - t) * x + t * y,
	degToRad: d => d * Math.PI / 180, radToDeg: r => r * 180 / Math.PI,
	euclideanModulo: (n, m) => ((n % m) + m) % m,
};
THREE.MathUtils = MathUtils;

// ---------------------------------------------------------------- Vector2
class Vector2 {
	constructor(x = 0, y = 0) { this.x = x; this.y = y; }
	get width() { return this.x; } set width(v) { this.x = v; }
	get height() { return this.y; } set height(v) { this.y = v; }
	set(x, y) { this.x = x; this.y = y; return this; }
	setScalar(s) { this.x = s; this.y = s; return this; }
	clone() { return new this.constructor(this.x, this.y); }
	copy(v) { this.x = v.x; this.y = v.y; return this; }
	add(v) { this.x += v.x; this.y += v.y; return this; }
	addScalar(s) { this.x += s; this.y += s; return this; }
	addVectors(a, b) { this.x = a.x + b.x; this.y = a.y + b.y; return this; }
	addScaledVector(v, s) { this.x += v.x * s; this.y += v.y * s; return this; }
	sub(v) { this.x -= v.x; this.y -= v.y; return this; }
	subVectors(a, b) { this.x = a.x - b.x; this.y = a.y - b.y; return this; }
	multiply(v) { this.x *= v.x; this.y *= v.y; return this; }
	multiplyScalar(s) { this.x *= s; this.y *= s; return this; }
	divideScalar(s) { return this.multiplyScalar(1 / s); }
	negate() { this.x = -this.x; this.y = -this.y; return this; }
	dot(v) { return this.x * v.x + this.y * v.y; }
	cross(v) { return this.x * v.y - this.y * v.x; }
	lengthSq() { return this.x * this.x + this.y * this.y; }
	length() { return Math.sqrt(this.x * this.x + this.y * this.y); }
	normalize() { return this.divideScalar(this.length() || 1); }
	angle() { return Math.atan2(-this.y, -this.x) + Math.PI; }
	distanceTo(v) { return Math.sqrt(this.distanceToSquared(v)); }
	distanceToSquared(v) { const dx = this.x - v.x, dy = this.y - v.y; return dx * dx + dy * dy; }
	setLength(l) { return this.normalize().multiplyScalar(l); }
	lerp(v, a) { this.x += (v.x - this.x) * a; this.y += (v.y - this.y) * a; return this; }
	lerpVectors(v1, v2, a) { this.x = v1.x + (v2.x - v1.x) * a; this.y = v1.y + (v2.y - v1.y) * a; return this; }
	rotateAround(center, angle) {
		const c = Math.cos(angle), s = Math.sin(angle), x = this.x - center.x, y = this.y - center.y;
		this.x = x * c - y * s + center.x; this.y = x * s + y * c + center.y; return this;
	}
	equals(v) { return v.x === this.x && v.y === this.y; }
	fromArray(a, o = 0) { this.x = a[o]; this.y = a[o + 1]; return this; }
	toArray(a = [], o = 0) { a[o] = this.x; a[o + 1] = this.y; return a; }
	fromBufferAttribute(attr, i) { this.x = attr.getX(i); this.y = attr.getY(i); return this; }
}
Vector2.prototype.isVector2 = true;

// ---------------------------------------------------------------- Vector3
class Vector3 {
	constructor(x = 0, y = 0, z = 0) { this.x = x; this.y = y; this.z = z; }
	set(x, y, z) { if (z === undefined) z = this.z; this.x = x; this.y = y; this.z = z; return this; }
	setScalar(s) { this.x = s; this.y = s; this.z = s; return this; }
	setX(x) { this.x = x; return this; } setY(y) { this.y = y; return this; } setZ(z) { this.z = z; return this; }
	clone() { return new this.constructor(this.x, this.y, this.z); }
	copy(v) { this.x = v.x; this.y = v.y; this.z = v.z; return this; }
	add(v) { this.x += v.x; this.y += v.y; this.z += v.z; return this; }
	addScalar(s) { this.x += s; this.y += s; this.z += s; return this; }
	addVectors(a, b) { this.x = a.x + b.x; this.y = a.y + b.y; this.z = a.z + b.z; return this; }
	addScaledVector(v, s) { this.x += v.x * s; this.y += v.y * s; this.z += v.z * s; return this; }
	sub(v) { this.x -= v.x; this.y -= v.y; this.z -= v.z; return this; }
	subScalar(s) { this.x -= s; this.y -= s; this.z -= s; return this; }
	subVectors(a, b) { this.x = a.x - b.x; this.y = a.y - b.y; this.z = a.z - b.z; return this; }
	multiply(v) { this.x *= v.x; this.y *= v.y; this.z *= v.z; return this; }
	multiplyScalar(s) { this.x *= s; this.y *= s; this.z *= s; return this; }
	multiplyVectors(a, b) { this.x = a.x * b.x; this.y = a.y * b.y; this.z = a.z * b.z; return this; }
	divide(v) { this.x /= v.x; this.y /= v.y; this.z /= v.z; return this; }
	divideScalar(s) { return this.multiplyScalar(1 / s); }
	applyEuler(e) { return this.applyQuaternion(_quaternionV.setFromEuler(e)); }
	applyAxisAngle(axis, angle) { return this.applyQuaternion(_quaternionV.setFromAxisAngle(axis, angle)); }
	applyMatrix3(m) {
		const x = this.x, y = this.y, z = this.z, e = m.elements;
		this.x = e[0] * x + e[3] * y + e[6] * z; this.y = e[1] * x + e[4] * y + e[7] * z; this.z = e[2] * x + e[5] * y + e[8] * z;
		return this;
	}
	applyNormalMatrix(m) { return this.applyMatrix3(m).normalize(); }
	applyMatrix4(m) {
		const x = this.x, y = this.y, z = this.z, e = m.elements;
		const w = 1 / (e[3] * x + e[7] * y + e[11] * z + e[15]);
		this.x = (e[0] * x + e[4] * y + e[8] * z + e[12]) * w;
		this.y = (e[1] * x + e[5] * y + e[9] * z + e[13]) * w;
		this.z = (e[2] * x + e[6] * y + e[10] * z + e[14]) * w;
		return this;
	}
	applyQuaternion(q) {
		const x = this.x, y = this.y, z = this.z, qx = q.x, qy = q.y, qz = q.z, qw = q.w;
		const ix = qw * x + qy * z - qz * y, iy = qw * y + qz * x - qx * z, iz = qw * z + qx * y - qy * x, iw = -qx * x - qy * y - qz * z;
		this.x = ix * qw + iw * -qx + iy * -qz - iz * -qy;
		this.y = iy * qw + iw * -qy + iz * -qx - ix * -qz;
		this.z = iz * qw + iw * -qz + ix * -qy - iy * -qx;
		return this;
	}
	transformDirection(m) {
		const x = this.x, y = this.y, z = this.z, e = m.elements;
		this.x = e[0] * x + e[4] * y + e[8] * z; this.y = e[1] * x + e[5] * y + e[9] * z; this.z = e[2] * x + e[6] * y + e[10] * z;
		return this.normalize();
	}
	min(v) { this.x = Math.min(this.x, v.x); this.y = Math.min(this.y, v.y); this.z = Math.min(this.z, v.z); return this; }
	max(v) { this.x = Math.max(this.x, v.x); this.y = Math.max(this.y, v.y); this.z = Math.max(this.z, v.z); return this; }
	negate() { this.x = -this.x; this.y = -this.y; this.z = -this.z; return this; }
	dot(v) { return this.x * v.x + this.y * v.y + this.z * v.z; }
	lengthSq() { return this.x * this.x + this.y * this.y + this.z * this.z; }
	length() { return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z); }
	normalize() { return this.divideScalar(this.length() || 1); }
	setLength(l) { return this.normalize().multiplyScalar(l); }
	lerp(v, a) { this.x += (v.x - this.x) * a; this.y += (v.y - this.y) * a; this.z += (v.z - this.z) * a; return this; }
	lerpVectors(v1, v2, a) { this.x = v1.x + (v2.x - v1.x) * a; this.y = v1.y + (v2.y - v1.y) * a; this.z = v1.z + (v2.z - v1.z) * a; return this; }
	cross(v) { return this.crossVectors(this, v); }
	crossVectors(a, b) {
		const ax = a.x, ay = a.y, az = a.z, bx = b.x, by = b.y, bz = b.z;
		this.x = ay * bz - az * by; this.y = az * bx - ax * bz; this.z = ax * by - ay * bx;
		return this;
	}
	projectOnVector(v) { const d = v.lengthSq(); if (d === 0) return this.set(0, 0, 0); return this.copy(v).multiplyScalar(v.dot(this) / d); }
	angleTo(v) { const d = Math.sqrt(this.lengthSq() * v.lengthSq()); if (d === 0) return Math.PI / 2; return Math.acos(clamp(this.dot(v) / d, -1, 1)); }
	distanceTo(v) { return Math.sqrt(this.distanceToSquared(v)); }
	distanceToSquared(v) { const dx = this.x - v.x, dy = this.y - v.y, dz = this.z - v.z; return dx * dx + dy * dy + dz * dz; }
	setFromMatrixPosition(m) { const e = m.elements; this.x = e[12]; this.y = e[13]; this.z = e[14]; return this; }
	setFromMatrixColumn(m, i) { return this.fromArray(m.elements, i * 4); }
	setFromMatrixScale(m) {
		const sx = this.setFromMatrixColumn(m, 0).length(), sy = this.setFromMatrixColumn(m, 1).length(), sz = this.setFromMatrixColumn(m, 2).length();
		this.x = sx; this.y = sy; this.z = sz; return this;
	}
	equals(v) { return v.x === this.x && v.y === this.y && v.z === this.z; }
	fromArray(a, o = 0) { this.x = a[o]; this.y = a[o + 1]; this.z = a[o + 2]; return this; }
	toArray(a = [], o = 0) { a[o] = this.x; a[o + 1] = this.y; a[o + 2] = this.z; return a; }
	fromBufferAttribute(attr, i) { this.x = attr.getX(i); this.y = attr.getY(i); this.z = attr.getZ(i); return this; }
}
Vector3.prototype.isVector3 = true;

// ---------------------------------------------------------------- Quaternion
class Quaternion {
	constructor(x = 0, y = 0, z = 0, w = 1) { this._x = x; this._y = y; this._z = z; this._w = w; this._onChangeCallback = function () {}; }
	get x() { return this._x; } set x(v) { this._x = v; this._onChangeCallback(); }
	get y() { return this._y; } set y(v) { this._y = v; this._onChangeCallback(); }
	get z() { return this._z; } set z(v) { this._z = v; this._onChangeCallback(); }
	get w() { return this._w; } set w(v) { this._w = v; this._onChangeCallback(); }
	set(x, y, z, w) { this._x = x; this._y = y; this._z = z; this._w = w; this._onChangeCallback(); return this; }
	clone() { return new this.constructor(this._x, this._y, this._z, this._w); }
	copy(q) { this._x = q.x; this._y = q.y; this._z = q.z; this._w = q.w; this._onChangeCallback(); return this; }
	setFromEuler(euler, update) {
		const x = euler._x, y = euler._y, z = euler._z, order = euler._order;
		const c1 = Math.cos(x / 2), c2 = Math.cos(y / 2), c3 = Math.cos(z / 2), s1 = Math.sin(x / 2), s2 = Math.sin(y / 2), s3 = Math.sin(z / 2);
		switch (order) {
			case 'XYZ': this._x = s1 * c2 * c3 + c1 * s2 * s3; this._y = c1 * s2 * c3 - s1 * c2 * s3; this._z = c1 * c2 * s3 + s1 * s2 * c3; this._w = c1 * c2 * c3 - s1 * s2 * s3; break;
			case 'YXZ': this._x = s1 * c2 * c3 + c1 * s2 * s3; this._y = c1 * s2 * c3 - s1 * c2 * s3; this._z = c1 * c2 * s3 - s1 * s2 * c3; this._w = c1 * c2 * c3 + s1 * s2 * s3; break;
			case 'ZXY': this._x = s1 * c2 * c3 - c1 * s2 * s3; this._y = c1 * s2 * c3 + s1 * c2 * s3; this._z = c1 * c2 * s3 + s1 * s2 * c3; this._w = c1 * c2 * c3 - s1 * s2 * s3; break;
			case 'ZYX': this._x = s1 * c2 * c3 - c1 * s2 * s3; this._y = c1 * s2 * c3 + s1 * c2 * s3; this._z = c1 * c2 * s3 - s1 * s2 * c3; this._w = c1 * c2 * c3 + s1 * s2 * s3; break;
			case 'YZX': this._x = s1 * c2 * c3 + c1 * s2 * s3; this._y = c1 * s2 * c3 + s1 * c2 * s3; this._z = c1 * c2 * s3 - s1 * s2 * c3; this._w = c1 * c2 * c3 - s1 * s2 * s3; break;
			case 'XZY': this._x = s1 * c2 * c3 - c1 * s2 * s3; this._y = c1 * s2 * c3 - s1 * c2 * s3; this._z = c1 * c2 * s3 + s1 * s2 * c3; this._w = c1 * c2 * c3 + s1 * s2 * s3; break;
			default: console.warn('THREE.Quaternion: .setFromEuler() encountered an unknown order: ' + order);
		}
		if (update !== false) this._onChangeCallback();
		return this;
	}
	setFromAxisAngle(axis, angle) {
		const h = angle / 2, s = Math.sin(h);
		this._x = axis.x * s; this._y = axis.y * s; this._z = axis.z * s; this._w = Math.cos(h);
		this._onChangeCallback(); return this;
	}
	setFromRotationMatrix(m) {
		const te = m.elements, m11 = te[0], m12 = te[4], m13 = te[8], m21 = te[1], m22 = te[5], m23 = te[9], m31 = te[2], m32 = te[6], m33 = te[10];
		const trace = m11 + m22 + m33;
		if (trace > 0) {
			const s = 0.5 / Math.sqrt(trace + 1.0);
			this._w = 0.25 / s; this._x = (m32 - m23) * s; this._y = (m13 - m31) * s; this._z = (m21 - m12) * s;
		} else if (m11 > m22 && m11 > m33) {
			const s = 2.0 * Math.sqrt(1.0 + m11 - m22 - m33);
			this._w = (m32 - m23) / s; this._x = 0.25 * s; this._y = (m12 + m21) / s; this._z = (m13 + m31) / s;
		} else if (m22 > m33) {
			const s = 2.0 * Math.sqrt(1.0 + m22 - m11 - m33);
			this._w = (m13 - m31) / s; this._x = (m12 + m21) / s; this._y = 0.25 * s; this._z = (m23 + m32) / s;
		} else {
			const s = 2.0 * Math.sqrt(1.0 + m33 - m11 - m22);
			this._w = (m21 - m12) / s; this._x = (m13 + m31) / s; this._y = (m23 + m32) / s; this._z = 0.25 * s;
		}
		this._onChangeCallback(); return this;
	}
	setFromUnitVectors(vFrom, vTo) {
		const EPS = 0.000001;
		let r = vFrom.dot(vTo) + 1;
		if (r < EPS) {
			r = 0;
			if (Math.abs(vFrom.x) > Math.abs(vFrom.z)) { this._x = -vFrom.y; this._y = vFrom.x; this._z = 0; this._w = r; }
			else { this._x = 0; this._y = -vFrom.z; this._z = vFrom.y; this._w = r; }
		} else {
			this._x = vFrom.y * vTo.z - vFrom.z * vTo.y; this._y = vFrom.z * vTo.x - vFrom.x * vTo.z; this._z = vFrom.x * vTo.y - vFrom.y * vTo.x; this._w = r;
		}
		return this.normalize();
	}
	invert() { return this.conjugate(); }
	conjugate() { this._x *= -1; this._y *= -1; this._z *= -1; this._onChangeCallback(); return this; }
	dot(v) { return this._x * v._x + this._y * v._y + this._z * v._z + this._w * v._w; }
	lengthSq() { return this._x * this._x + this._y * this._y + this._z * this._z + this._w * this._w; }
	length() { return Math.sqrt(this.lengthSq()); }
	normalize() {
		let l = this.length();
		if (l === 0) { this._x = 0; this._y = 0; this._z = 0; this._w = 1; }
		else { l = 1 / l; this._x = this._x * l; this._y = this._y * l; this._z = this._z * l; this._w = this._w * l; }
		this._onChangeCallback(); return this;
	}
	multiply(q) { return this.multiplyQuaternions(this, q); }
	premultiply(q) { return this.multiplyQuaternions(q, this); }
	multiplyQuaternions(a, b) {
		const qax = a._x, qay = a._y, qaz = a._z, qaw = a._w, qbx = b._x, qby = b._y, qbz = b._z, qbw = b._w;
		this._x = qax * qbw + qaw * qbx + qay * qbz - qaz * qby;
		this._y = qay * qbw + qaw * qby + qaz * qbx - qax * qbz;
		this._z = qaz * qbw + qaw * qbz + qax * qby - qay * qbx;
		this._w = qaw * qbw - qax * qbx - qay * qby - qaz * qbz;
		this._onChangeCallback(); return this;
	}
	equals(q) { return q._x === this._x && q._y === this._y && q._z === this._z && q._w === this._w; }
	toArray(a = [], o = 0) { a[o] = this._x; a[o + 1] = this._y; a[o + 2] = this._z; a[o + 3] = this._w; return a; }
	_onChange(cb) { this._onChangeCallback = cb; return this; }
}
Quaternion.prototype.isQuaternion = true;
const _quaternionV = new Quaternion();

// ---------------------------------------------------------------- Euler
class Euler {
	constructor(x = 0, y = 0, z = 0, order = 'XYZ') { this._x = x; this._y = y; this._z = z; this._order = order; this._onChangeCallback = function () {}; }
	get x() { return this._x; } set x(v) { this._x = v; this._onChangeCallback(); }
	get y() { return this._y; } set y(v) { this._y = v; this._onChangeCallback(); }
	get z() { return this._z; } set z(v) { this._z = v; this._onChangeCallback(); }
	get order() { return this._order; } set order(v) { this._order = v; this._onChangeCallback(); }
	set(x, y, z, order) { this._x = x; this._y = y; this._z = z; this._order = order || this._order; this._onChangeCallback(); return this; }
	clone() { return new this.constructor(this._x, this._y, this._z, this._order); }
	copy(e) { this._x = e._x; this._y = e._y; this._z = e._z; this._order = e._order; this._onChangeCallback(); return this; }
	setFromRotationMatrix(m, order, update) {
		const te = m.elements, m11 = te[0], m12 = te[4], m13 = te[8], m21 = te[1], m22 = te[5], m23 = te[9], m31 = te[2], m32 = te[6], m33 = te[10];
		order = order || this._order;
		switch (order) {
			case 'XYZ':
				this._y = Math.asin(clamp(m13, -1, 1));
				if (Math.abs(m13) < 0.9999999) { this._x = Math.atan2(-m23, m33); this._z = Math.atan2(-m12, m11); }
				else { this._x = Math.atan2(m32, m22); this._z = 0; }
				break;
			case 'YXZ':
				this._x = Math.asin(-clamp(m23, -1, 1));
				if (Math.abs(m23) < 0.9999999) { this._y = Math.atan2(m13, m33); this._z = Math.atan2(m21, m22); }
				else { this._y = Math.atan2(-m31, m11); this._z = 0; }
				break;
			case 'ZXY':
				this._x = Math.asin(clamp(m32, -1, 1));
				if (Math.abs(m32) < 0.9999999) { this._y = Math.atan2(-m31, m33); this._z = Math.atan2(-m12, m22); }
				else { this._y = 0; this._z = Math.atan2(m21, m11); }
				break;
			case 'ZYX':
				this._y = Math.asin(-clamp(m31, -1, 1));
				if (Math.abs(m31) < 0.9999999) { this._x = Math.atan2(m32, m33); this._z = Math.atan2(m21, m11); }
				else { this._x = 0; this._z = Math.atan2(-m12, m22); }
				break;
			case 'YZX':
				this._z = Math.asin(clamp(m21, -1, 1));
				if (Math.abs(m21) < 0.9999999) { this._x = Math.atan2(-m23, m22); this._y = Math.atan2(-m31, m11); }
				else { this._x = 0; this._y = Math.atan2(m13, m33); }
				break;
			case 'XZY':
				this._z = Math.asin(-clamp(m12, -1, 1));
				if (Math.abs(m12) < 0.9999999) { this._x = Math.atan2(m32, m22); this._y = Math.atan2(m13, m11); }
				else { this._x = Math.atan2(-m23, m33); this._y = 0; }
				break;
			default: console.warn('THREE.Euler: .setFromRotationMatrix() encountered an unknown order: ' + order);
		}
		this._order = order;
		if (update !== false) this._onChangeCallback();
		return this;
	}
	setFromQuaternion(q, order, update) { _matrixE.makeRotationFromQuaternion(q); return this.setFromRotationMatrix(_matrixE, order, update); }
	toArray(a = [], o = 0) { a[o] = this._x; a[o + 1] = this._y; a[o + 2] = this._z; a[o + 3] = this._order; return a; }
	_onChange(cb) { this._onChangeCallback = cb; return this; }
}
Euler.prototype.isEuler = true;
Euler.DefaultOrder = 'XYZ';

// ---------------------------------------------------------------- Matrix3
class Matrix3 {
	constructor() { this.elements = [1, 0, 0, 0, 1, 0, 0, 0, 1]; }
	set(n11, n12, n13, n21, n22, n23, n31, n32, n33) {
		const te = this.elements;
		te[0] = n11; te[1] = n21; te[2] = n31; te[3] = n12; te[4] = n22; te[5] = n32; te[6] = n13; te[7] = n23; te[8] = n33;
		return this;
	}
	identity() { return this.set(1, 0, 0, 0, 1, 0, 0, 0, 1); }
	clone() { return new this.constructor().fromArray(this.elements); }
	copy(m) { const te = this.elements, me = m.elements; for (let i = 0; i < 9; i++) te[i] = me[i]; return this; }
	fromArray(a, o = 0) { for (let i = 0; i < 9; i++) this.elements[i] = a[i + o]; return this; }
	setFromMatrix4(m) { const me = m.elements; return this.set(me[0], me[4], me[8], me[1], me[5], me[9], me[2], me[6], me[10]); }
	determinant() {
		const te = this.elements, a = te[0], b = te[1], c = te[2], d = te[3], e = te[4], f = te[5], g = te[6], h = te[7], i = te[8];
		return a * e * i - a * f * h - b * d * i + b * f * g + c * d * h - c * e * g;
	}
	invert() {
		const te = this.elements, n11 = te[0], n21 = te[1], n31 = te[2], n12 = te[3], n22 = te[4], n32 = te[5], n13 = te[6], n23 = te[7], n33 = te[8];
		const t11 = n33 * n22 - n32 * n23, t12 = n32 * n13 - n33 * n12, t13 = n23 * n12 - n22 * n13;
		const det = n11 * t11 + n21 * t12 + n31 * t13;
		if (det === 0) return this.set(0, 0, 0, 0, 0, 0, 0, 0, 0);
		const detInv = 1 / det;
		te[0] = t11 * detInv; te[1] = (n31 * n23 - n33 * n21) * detInv; te[2] = (n32 * n21 - n31 * n22) * detInv;
		te[3] = t12 * detInv; te[4] = (n33 * n11 - n31 * n13) * detInv; te[5] = (n31 * n12 - n32 * n11) * detInv;
		te[6] = t13 * detInv; te[7] = (n21 * n13 - n23 * n11) * detInv; te[8] = (n22 * n11 - n21 * n12) * detInv;
		return this;
	}
	transpose() {
		const m = this.elements; let tmp;
		tmp = m[1]; m[1] = m[3]; m[3] = tmp; tmp = m[2]; m[2] = m[6]; m[6] = tmp; tmp = m[5]; m[5] = m[7]; m[7] = tmp;
		return this;
	}
	getNormalMatrix(matrix4) { return this.setFromMatrix4(matrix4).invert().transpose(); }
}
Matrix3.prototype.isMatrix3 = true;

// ---------------------------------------------------------------- Matrix4
class Matrix4 {
	constructor() { this.elements = [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]; }
	set(n11, n12, n13, n14, n21, n22, n23, n24, n31, n32, n33, n34, n41, n42, n43, n44) {
		const te = this.elements;
		te[0] = n11; te[4] = n12; te[8] = n13; te[12] = n14;
		te[1] = n21; te[5] = n22; te[9] = n23; te[13] = n24;
		te[2] = n31; te[6] = n32; te[10] = n33; te[14] = n34;
		te[3] = n41; te[7] = n42; te[11] = n43; te[15] = n44;
		return this;
	}
	identity() { return this.set(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1); }
	clone() { return new Matrix4().fromArray(this.elements); }
	copy(m) { const te = this.elements, me = m.elements; for (let i = 0; i < 16; i++) te[i] = me[i]; return this; }
	fromArray(a, o = 0) { for (let i = 0; i < 16; i++) this.elements[i] = a[i + o]; return this; }
	toArray(a = [], o = 0) { for (let i = 0; i < 16; i++) a[o + i] = this.elements[i]; return a; }
	makeRotationFromQuaternion(q) { return this.compose(_zero, q, _one); }
	compose(position, quaternion, scale) {
		const te = this.elements, x = quaternion._x, y = quaternion._y, z = quaternion._z, w = quaternion._w;
		const x2 = x + x, y2 = y + y, z2 = z + z, xx = x * x2, xy = x * y2, xz = x * z2, yy = y * y2, yz = y * z2, zz = z * z2, wx = w * x2, wy = w * y2, wz = w * z2;
		const sx = scale.x, sy = scale.y, sz = scale.z;
		te[0] = (1 - (yy + zz)) * sx; te[1] = (xy + wz) * sx; te[2] = (xz - wy) * sx; te[3] = 0;
		te[4] = (xy - wz) * sy; te[5] = (1 - (xx + zz)) * sy; te[6] = (yz + wx) * sy; te[7] = 0;
		te[8] = (xz + wy) * sz; te[9] = (yz - wx) * sz; te[10] = (1 - (xx + yy)) * sz; te[11] = 0;
		te[12] = position.x; te[13] = position.y; te[14] = position.z; te[15] = 1;
		return this;
	}
	multiply(m) { return this.multiplyMatrices(this, m); }
	premultiply(m) { return this.multiplyMatrices(m, this); }
	multiplyMatrices(a, b) {
		const ae = a.elements, be = b.elements, te = this.elements;
		const a11 = ae[0], a12 = ae[4], a13 = ae[8], a14 = ae[12], a21 = ae[1], a22 = ae[5], a23 = ae[9], a24 = ae[13];
		const a31 = ae[2], a32 = ae[6], a33 = ae[10], a34 = ae[14], a41 = ae[3], a42 = ae[7], a43 = ae[11], a44 = ae[15];
		const b11 = be[0], b12 = be[4], b13 = be[8], b14 = be[12], b21 = be[1], b22 = be[5], b23 = be[9], b24 = be[13];
		const b31 = be[2], b32 = be[6], b33 = be[10], b34 = be[14], b41 = be[3], b42 = be[7], b43 = be[11], b44 = be[15];
		te[0] = a11 * b11 + a12 * b21 + a13 * b31 + a14 * b41; te[4] = a11 * b12 + a12 * b22 + a13 * b32 + a14 * b42;
		te[8] = a11 * b13 + a12 * b23 + a13 * b33 + a14 * b43; te[12] = a11 * b14 + a12 * b24 + a13 * b34 + a14 * b44;
		te[1] = a21 * b11 + a22 * b21 + a23 * b31 + a24 * b41; te[5] = a21 * b12 + a22 * b22 + a23 * b32 + a24 * b42;
		te[9] = a21 * b13 + a22 * b23 + a23 * b33 + a24 * b43; te[13] = a21 * b14 + a22 * b24 + a23 * b34 + a24 * b44;
		te[2] = a31 * b11 + a32 * b21 + a33 * b31 + a34 * b41; te[6] = a31 * b12 + a32 * b22 + a33 * b32 + a34 * b42;
		te[10] = a31 * b13 + a32 * b23 + a33 * b33 + a34 * b43; te[14] = a31 * b14 + a32 * b24 + a33 * b34 + a34 * b44;
		te[3] = a41 * b11 + a42 * b21 + a43 * b31 + a44 * b41; te[7] = a41 * b12 + a42 * b22 + a43 * b32 + a44 * b42;
		te[11] = a41 * b13 + a42 * b23 + a43 * b33 + a44 * b43; te[15] = a41 * b14 + a42 * b24 + a43 * b34 + a44 * b44;
		return this;
	}
	multiplyScalar(s) { for (let i = 0; i < 16; i++) this.elements[i] *= s; return this; }
	determinant() {
		const te = this.elements;
		const n11 = te[0], n12 = te[4], n13 = te[8], n14 = te[12], n21 = te[1], n22 = te[5], n23 = te[9], n24 = te[13];
		const n31 = te[2], n32 = te[6], n33 = te[10], n34 = te[14], n41 = te[3], n42 = te[7], n43 = te[11], n44 = te[15];
		return (
			n41 * (+n14 * n23 * n32 - n13 * n24 * n32 - n14 * n22 * n33 + n12 * n24 * n33 + n13 * n22 * n34 - n12 * n23 * n34) +
			n42 * (+n11 * n23 * n34 - n11 * n24 * n33 + n14 * n21 * n33 - n13 * n21 * n34 + n13 * n24 * n31 - n14 * n23 * n31) +
			n43 * (+n11 * n24 * n32 - n11 * n22 * n34 - n14 * n21 * n32 + n12 * n21 * n34 + n14 * n22 * n31 - n12 * n24 * n31) +
			n44 * (-n13 * n22 * n31 - n11 * n23 * n32 + n11 * n22 * n33 + n13 * n21 * n32 - n12 * n21 * n33 + n12 * n23 * n31)
		);
	}
	transpose() {
		const te = this.elements; let tmp;
		tmp = te[1]; te[1] = te[4]; te[4] = tmp; tmp = te[2]; te[2] = te[8]; te[8] = tmp; tmp = te[6]; te[6] = te[9]; te[9] = tmp;
		tmp = te[3]; te[3] = te[12]; te[12] = tmp; tmp = te[7]; te[7] = te[13]; te[13] = tmp; tmp = te[11]; te[11] = te[14]; te[14] = tmp;
		return this;
	}
	setPosition(x, y, z) { const te = this.elements; if (x.isVector3) { te[12] = x.x; te[13] = x.y; te[14] = x.z; } else { te[12] = x; te[13] = y; te[14] = z; } return this; }
	invert() {
		const te = this.elements;
		const n11 = te[0], n21 = te[1], n31 = te[2], n41 = te[3], n12 = te[4], n22 = te[5], n32 = te[6], n42 = te[7];
		const n13 = te[8], n23 = te[9], n33 = te[10], n43 = te[11], n14 = te[12], n24 = te[13], n34 = te[14], n44 = te[15];
		const t11 = n23 * n34 * n42 - n24 * n33 * n42 + n24 * n32 * n43 - n22 * n34 * n43 - n23 * n32 * n44 + n22 * n33 * n44;
		const t12 = n14 * n33 * n42 - n13 * n34 * n42 - n14 * n32 * n43 + n12 * n34 * n43 + n13 * n32 * n44 - n12 * n33 * n44;
		const t13 = n13 * n24 * n42 - n14 * n23 * n42 + n14 * n22 * n43 - n12 * n24 * n43 - n13 * n22 * n44 + n12 * n23 * n44;
		const t14 = n14 * n23 * n32 - n13 * n24 * n32 - n14 * n22 * n33 + n12 * n24 * n33 + n13 * n22 * n34 - n12 * n23 * n34;
		const det = n11 * t11 + n21 * t12 + n31 * t13 + n41 * t14;
		if (det === 0) return this.set(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		const detInv = 1 / det;
		te[0] = t11 * detInv;
		te[1] = (n24 * n33 * n41 - n23 * n34 * n41 - n24 * n31 * n43 + n21 * n34 * n43 + n23 * n31 * n44 - n21 * n33 * n44) * detInv;
		te[2] = (n22 * n34 * n41 - n24 * n32 * n41 + n24 * n31 * n42 - n21 * n34 * n42 - n22 * n31 * n44 + n21 * n32 * n44) * detInv;
		te[3] = (n23 * n32 * n41 - n22 * n33 * n41 - n23 * n31 * n42 + n21 * n33 * n42 + n22 * n31 * n43 - n21 * n32 * n43) * detInv;
		te[4] = t12 * detInv;
		te[5] = (n13 * n34 * n41 - n14 * n33 * n41 + n14 * n31 * n43 - n11 * n34 * n43 - n13 * n31 * n44 + n11 * n33 * n44) * detInv;
		te[6] = (n14 * n32 * n41 - n12 * n34 * n41 - n14 * n31 * n42 + n11 * n34 * n42 + n12 * n31 * n44 - n11 * n32 * n44) * detInv;
		te[7] = (n12 * n33 * n41 - n13 * n32 * n41 + n13 * n31 * n42 - n11 * n33 * n42 - n12 * n31 * n43 + n11 * n32 * n43) * detInv;
		te[8] = t13 * detInv;
		te[9] = (n14 * n23 * n41 - n13 * n24 * n41 - n14 * n21 * n43 + n11 * n24 * n43 + n13 * n21 * n44 - n11 * n23 * n44) * detInv;
		te[10] = (n12 * n24 * n41 - n14 * n22 * n41 + n14 * n21 * n42 - n11 * n24 * n42 - n12 * n21 * n44 + n11 * n22 * n44) * detInv;
		te[11] = (n13 * n22 * n41 - n12 * n23 * n41 - n13 * n21 * n42 + n11 * n23 * n42 + n12 * n21 * n43 - n11 * n22 * n43) * detInv;
		te[12] = t14 * detInv;
		te[13] = (n13 * n24 * n31 - n14 * n23 * n31 + n14 * n21 * n33 - n11 * n24 * n33 - n13 * n21 * n34 + n11 * n23 * n34) * detInv;
		te[14] = (n14 * n22 * n31 - n12 * n24 * n31 - n14 * n21 * n32 + n11 * n24 * n32 + n12 * n21 * n34 - n11 * n22 * n34) * detInv;
		te[15] = (n12 * n23 * n31 - n13 * n22 * n31 + n13 * n21 * n32 - n11 * n23 * n32 - n12 * n21 * n33 + n11 * n22 * n33) * detInv;
		return this;
	}
	makeTranslation(x, y, z) { return this.set(1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1); }
	makeRotationX(t) { const c = Math.cos(t), s = Math.sin(t); return this.set(1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0, 0, 0, 0, 1); }
	makeRotationY(t) { const c = Math.cos(t), s = Math.sin(t); return this.set(c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0, 0, 0, 0, 1); }
	makeRotationZ(t) { const c = Math.cos(t), s = Math.sin(t); return this.set(c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1); }
	makeRotationAxis(axis, angle) {
		const c = Math.cos(angle), s = Math.sin(angle), t = 1 - c, x = axis.x, y = axis.y, z = axis.z, tx = t * x, ty = t * y;
		return this.set(tx * x + c, tx * y - s * z, tx * z + s * y, 0, tx * y + s * z, ty * y + c, ty * z - s * x, 0, tx * z - s * y, ty * z + s * x, t * z * z + c, 0, 0, 0, 0, 1);
	}
	makeScale(x, y, z) { return this.set(x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0, 0, 0, 0, 1); }
}
Matrix4.prototype.isMatrix4 = true;
const _zero = new Vector3(0, 0, 0), _one = new Vector3(1, 1, 1), _matrixE = new Matrix4();

// ---------------------------------------------------------------- Color
const _colorKeywords = { white: 0xffffff, black: 0x000000, red: 0xff0000, green: 0x008000, blue: 0x0000ff, yellow: 0xffff00, gray: 0x808080, grey: 0x808080, orange: 0xffa500 };
function SRGBToLinear(c) { return (c < 0.04045) ? c * 0.0773993808 : Math.pow(c * 0.9478672986 + 0.0521327014, 2.4); }
function LinearToSRGB(c) { return (c < 0.0031308) ? c * 12.92 : 1.055 * (Math.pow(c, 0.41666)) - 0.055; }
class Color {
	constructor(r, g, b) { this.r = 1; this.g = 1; this.b = 1; if (g === undefined && b === undefined) return this.set(r); return this.setRGB(r, g, b); }
	set(value) {
		if (value && value.isColor) this.copy(value);
		else if (typeof value === 'number') this.setHex(value);
		else if (typeof value === 'string') this.setStyle(value);
		return this;
	}
	setScalar(s) { this.r = s; this.g = s; this.b = s; return this; }
	setHex(hex) { hex = Math.floor(hex); this.r = (hex >> 16 & 255) / 255; this.g = (hex >> 8 & 255) / 255; this.b = (hex & 255) / 255; return this; }
	setRGB(r, g, b) { this.r = r; this.g = g; this.b = b; return this; }
	setStyle(style) {
		let m;
		if ((m = /^\#([A-Fa-f\d]+)$/.exec(style))) {
			const hex = m[1];
			if (hex.length === 3) { this.r = parseInt(hex.charAt(0) + hex.charAt(0), 16) / 255; this.g = parseInt(hex.charAt(1) + hex.charAt(1), 16) / 255; this.b = parseInt(hex.charAt(2) + hex.charAt(2), 16) / 255; return this; }
			if (hex.length === 6) { this.r = parseInt(hex.charAt(0) + hex.charAt(1), 16) / 255; this.g = parseInt(hex.charAt(2) + hex.charAt(3), 16) / 255; this.b = parseInt(hex.charAt(4) + hex.charAt(5), 16) / 255; return this; }
		}
		if ((m = /^rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*[\d.]+\s*)?\)$/.exec(style))) {
			this.r = Math.min(255, parseInt(m[1], 10)) / 255; this.g = Math.min(255, parseInt(m[2], 10)) / 255; this.b = Math.min(255, parseInt(m[3], 10)) / 255; return this;
		}
		if (style && style.length > 0) {
			const hex = _colorKeywords[style.toLowerCase()];
			if (hex !== undefined) this.setHex(hex); else console.warn('THREE.Color: Unknown color ' + style);
		}
		return this;
	}
	clone() { return new this.constructor(this.r, this.g, this.b); }
	copy(c) { this.r = c.r; this.g = c.g; this.b = c.b; return this; }
	copySRGBToLinear(c) { this.r = SRGBToLinear(c.r); this.g = SRGBToLinear(c.g); this.b = SRGBToLinear(c.b); return this; }
	copyLinearToSRGB(c) { this.r = LinearToSRGB(c.r); this.g = LinearToSRGB(c.g); this.b = LinearToSRGB(c.b); return this; }
	convertSRGBToLinear() { return this.copySRGBToLinear(this); }
	convertLinearToSRGB() { return this.copyLinearToSRGB(this); }
	getHex() { return (this.r * 255) << 16 ^ (this.g * 255) << 8 ^ (this.b * 255) << 0; }
	getHexString() { return ('000000' + this.getHex().toString(16)).slice(-6); }
	add(c) { this.r += c.r; this.g += c.g; this.b += c.b; return this; }
	multiply(c) { this.r *= c.r; this.g *= c.g; this.b *= c.b; return this; }
	multiplyScalar(s) { this.r *= s; this.g *= s; this.b *= s; return this; }
	lerp(c, a) { this.r += (c.r - this.r) * a; this.g += (c.g - this.g) * a; this.b += (c.b - this.b) * a; return this; }
	equals(c) { return c.r === this.r && c.g === this.g && c.b === this.b; }
	toArray(a = [], o = 0) { a[o] = this.r; a[o + 1] = this.g; a[o + 2] = this.b; return a; }
}
Color.prototype.isColor = true;
Color.prototype.r = 1; Color.prototype.g = 1; Color.prototype.b = 1;

// ---------------------------------------------------------------- Object3D, Group, Mesh, Sprite
let _object3DId = 0;
const _q1 = new Quaternion(), _xAxis = new Vector3(1, 0, 0), _yAxis = new Vector3(0, 1, 0), _zAxis = new Vector3(0, 0, 1), _v1 = new Vector3();
class Object3D {
	constructor() {
		Object.defineProperty(this, 'id', { value: _object3DId++ });
		this.uuid = generateUUID(); this.name = ''; this.type = 'Object3D'; this.parent = null; this.children = [];
		this.up = Object3D.DefaultUp.clone();
		const position = new Vector3(), rotation = new Euler(), quaternion = new Quaternion(), scale = new Vector3(1, 1, 1);
		rotation._onChange(function () { quaternion.setFromEuler(rotation, false); });
		quaternion._onChange(function () { rotation.setFromQuaternion(quaternion, undefined, false); });
		Object.defineProperties(this, {
			position: { configurable: true, enumerable: true, value: position },
			rotation: { configurable: true, enumerable: true, value: rotation },
			quaternion: { configurable: true, enumerable: true, value: quaternion },
			scale: { configurable: true, enumerable: true, value: scale },
		});
		this.matrix = new Matrix4(); this.matrixWorld = new Matrix4();
		this.matrixAutoUpdate = Object3D.DefaultMatrixAutoUpdate; this.matrixWorldNeedsUpdate = false;
		this.visible = true; this.castShadow = false; this.receiveShadow = false; this.frustumCulled = true; this.renderOrder = 0;
		this.animations = []; this.userData = {};
	}
	applyMatrix4() { throw new Error('three_subset: Object3D.applyMatrix4 is not supported'); }
	applyQuaternion(q) { this.quaternion.premultiply(q); return this; }
	setRotationFromAxisAngle(axis, angle) { this.quaternion.setFromAxisAngle(axis, angle); }
	setRotationFromEuler(euler) { this.quaternion.setFromEuler(euler, true); }
	setRotationFromQuaternion(q) { this.quaternion.copy(q); }
	rotateOnAxis(axis, angle) { _q1.setFromAxisAngle(axis, angle); this.quaternion.multiply(_q1); return this; }
	rotateOnWorldAxis(axis, angle) { _q1.setFromAxisAngle(axis, angle); this.quaternion.premultiply(_q1); return this; }
	rotateX(a) { return this.rotateOnAxis(_xAxis, a); }
	rotateY(a) { return this.rotateOnAxis(_yAxis, a); }
	rotateZ(a) { return this.rotateOnAxis(_zAxis, a); }
	translateOnAxis(axis, distance) { _v1.copy(axis).applyQuaternion(this.quaternion); this.position.add(_v1.multiplyScalar(distance)); return this; }
	translateX(d) { return this.translateOnAxis(_xAxis, d); }
	translateY(d) { return this.translateOnAxis(_yAxis, d); }
	translateZ(d) { return this.translateOnAxis(_zAxis, d); }
	localToWorld(v) { return v.applyMatrix4(this.matrixWorld); }
	lookAt() { throw new Error('three_subset: Object3D.lookAt is not supported'); }
	add(object) {
		if (arguments.length > 1) { for (let i = 0; i < arguments.length; i++) this.add(arguments[i]); return this; }
		if (object === this) { console.error('THREE.Object3D.add: object can\'t be added as a child of itself.', object); return this; }
		if (object && object.isObject3D) {
			if (object.parent !== null) object.parent.remove(object);
			object.parent = this; this.children.push(object);
		} else console.error('THREE.Object3D.add: object not an instance of THREE.Object3D.', object);
		return this;
	}
	remove(object) {
		if (arguments.length > 1) { for (let i = 0; i < arguments.length; i++) this.remove(arguments[i]); return this; }
		const index = this.children.indexOf(object);
		if (index !== -1) { object.parent = null; this.children.splice(index, 1); }
		return this;
	}
	clear() { for (const c of this.children) c.parent = null; this.children.length = 0; return this; }
	traverse(cb) { cb(this); for (const c of this.children) c.traverse(cb); }
	traverseVisible(cb) { if (this.visible === false) return; cb(this); for (const c of this.children) c.traverseVisible(cb); }
	getObjectByName(name) { if (this.name === name) return this; for (const c of this.children) { const o = c.getObjectByName(name); if (o !== undefined) return o; } return undefined; }
	getWorldPosition(target) { this.updateWorldMatrix(true, false); return target.setFromMatrixPosition(this.matrixWorld); }
	updateMatrix() { this.matrix.compose(this.position, this.quaternion, this.scale); this.matrixWorldNeedsUpdate = true; }
	updateMatrixWorld(force) {
		if (this.matrixAutoUpdate) this.updateMatrix();
		if (this.matrixWorldNeedsUpdate || force) {
			if (this.parent === null) this.matrixWorld.copy(this.matrix);
			else this.matrixWorld.multiplyMatrices(this.parent.matrixWorld, this.matrix);
			this.matrixWorldNeedsUpdate = false; force = true;
		}
		const children = this.children;
		for (let i = 0, l = children.length; i < l; i++) children[i].updateMatrixWorld(force);
	}
	updateWorldMatrix(updateParents, updateChildren) {
		const parent = this.parent;
		if (updateParents === true && parent !== null) parent.updateWorldMatrix(true, false);
		if (this.matrixAutoUpdate) this.updateMatrix();
		if (this.parent === null) this.matrixWorld.copy(this.matrix); else this.matrixWorld.multiplyMatrices(this.parent.matrixWorld, this.matrix);
		if (updateChildren === true) for (const c of this.children) c.updateWorldMatrix(false, true);
	}
}
Object3D.DefaultUp = new Vector3(0, 1, 0);
Object3D.DefaultMatrixAutoUpdate = true;
Object3D.prototype.isObject3D = true;
class Group extends Object3D { constructor() { super(); this.type = 'Group'; } }
Group.prototype.isGroup = true;
class Scene extends Object3D { constructor() { super(); this.type = 'Scene'; this.background = null; } }
Scene.prototype.isScene = true;
class Mesh extends Object3D {
	constructor(geometry = new BufferGeometry(), material = new MeshBasicMaterial()) { super(); this.type = 'Mesh'; this.geometry = geometry; this.material = material; }
}
Mesh.prototype.isMesh = true;
class Sprite extends Object3D {
	constructor(material) { super(); this.type = 'Sprite'; this.geometry = new BufferGeometry(); this.material = (material !== undefined) ? material : new SpriteMaterial(); this.center = new Vector2(0.5, 0.5); }
}
Sprite.prototype.isSprite = true;

// ---------------------------------------------------------------- BufferAttribute
const _vectorA = new Vector3();
class BufferAttribute {
	constructor(array, itemSize, normalized) {
		if (Array.isArray(array)) throw new TypeError('THREE.BufferAttribute: array should be a Typed Array.');
		this.name = ''; this.array = array; this.itemSize = itemSize;
		this.count = array !== undefined ? array.length / itemSize : 0;
		this.normalized = normalized === true; this.usage = 35044; this.updateRange = { offset: 0, count: -1 }; this.version = 0;
	}
	set needsUpdate(value) { if (value === true) this.version++; }
	setUsage(u) { this.usage = u; return this; }
	copy(source) { this.name = source.name; this.array = new source.array.constructor(source.array); this.itemSize = source.itemSize; this.count = source.count; this.normalized = source.normalized; this.usage = source.usage; return this; }
	clone() { return new this.constructor(this.array, this.itemSize).copy(this); }
	copyArray(array) { this.array.set(array); return this; }
	applyMatrix3(m) {
		for (let i = 0, l = this.count; i < l; i++) { _vectorA.fromBufferAttribute(this, i); _vectorA.applyMatrix3(m); this.setXYZ(i, _vectorA.x, _vectorA.y, _vectorA.z); }
		return this;
	}
	applyMatrix4(m) {
		for (let i = 0, l = this.count; i < l; i++) { _vectorA.x = this.getX(i); _vectorA.y = this.getY(i); _vectorA.z = this.getZ(i); _vectorA.applyMatrix4(m); this.setXYZ(i, _vectorA.x, _vectorA.y, _vectorA.z); }
		return this;
	}
	applyNormalMatrix(m) {
		for (let i = 0, l = this.count; i < l; i++) { _vectorA.x = this.getX(i); _vectorA.y = this.getY(i); _vectorA.z = this.getZ(i); _vectorA.applyNormalMatrix(m); this.setXYZ(i, _vectorA.x, _vectorA.y, _vectorA.z); }
		return this;
	}
	transformDirection(m) {
		for (let i = 0, l = this.count; i < l; i++) { _vectorA.x = this.getX(i); _vectorA.y = this.getY(i); _vectorA.z = this.getZ(i); _vectorA.transformDirection(m); this.setXYZ(i, _vectorA.x, _vectorA.y, _vectorA.z); }
		return this;
	}
	set(value, offset = 0) { this.array.set(value, offset); return this; }
	getX(i) { return this.array[i * this.itemSize]; } setX(i, x) { this.array[i * this.itemSize] = x; return this; }
	getY(i) { return this.array[i * this.itemSize + 1]; } setY(i, y) { this.array[i * this.itemSize + 1] = y; return this; }
	getZ(i) { return this.array[i * this.itemSize + 2]; } setZ(i, z) { this.array[i * this.itemSize + 2] = z; return this; }
	getW(i) { return this.array[i * this.itemSize + 3]; } setW(i, w) { this.array[i * this.itemSize + 3] = w; return this; }
	setXY(i, x, y) { i *= this.itemSize; this.array[i] = x; this.array[i + 1] = y; return this; }
	setXYZ(i, x, y, z) { i *= this.itemSize; this.array[i] = x; this.array[i + 1] = y; this.array[i + 2] = z; return this; }
	setXYZW(i, x, y, z, w) { i *= this.itemSize; this.array[i] = x; this.array[i + 1] = y; this.array[i + 2] = z; this.array[i + 3] = w; return this; }
}
BufferAttribute.prototype.isBufferAttribute = true;
class Float32BufferAttribute extends BufferAttribute { constructor(array, itemSize, normalized) { super(new Float32Array(array), itemSize, normalized); } }
class Uint16BufferAttribute extends BufferAttribute { constructor(array, itemSize, normalized) { super(new Uint16Array(array), itemSize, normalized); } }
class Uint32BufferAttribute extends BufferAttribute { constructor(array, itemSize, normalized) { super(new Uint32Array(array), itemSize, normalized); } }
// present so BufferGeometryUtils.js can refer to them; nothing here builds interleaved data
class InterleavedBuffer { constructor(array, stride) { this.array = array; this.stride = stride; this.count = array !== undefined ? array.length / stride : 0; } }
InterleavedBuffer.prototype.isInterleavedBuffer = true;
class InterleavedBufferAttribute {
	constructor(interleavedBuffer, itemSize, offset, normalized) { this.data = interleavedBuffer; this.itemSize = itemSize; this.offset = offset; this.normalized = normalized === true; }
	get count() { return this.data.count; }
	get array() { return this.data.array; }
	getX(i) { return this.data.array[i * this.data.stride + this.offset]; }
	getY(i) { return this.data.array[i * this.data.stride + this.offset + 1]; }
	getZ(i) { return this.data.array[i * this.data.stride + this.offset + 2]; }
	getW(i) { return this.data.array[i * this.data.stride + this.offset + 3]; }
}
InterleavedBufferAttribute.prototype.isInterleavedBufferAttribute = true;

// ---------------------------------------------------------------- BufferGeometry
let _geometryId = 0;
const _m1 = new Matrix4();
function arrayMax(array) { if (array.length === 0) return -Infinity; let max = array[0]; for (let i = 1, l = array.length; i < l; ++i) if (array[i] > max) max = array[i]; return max; }
class BufferGeometry {
	constructor() {
		Object.defineProperty(this, 'id', { value: _geometryId++ });
		this.uuid = generateUUID(); this.name = ''; this.type = 'BufferGeometry';
		this.index = null; this.attributes = {}; this.morphAttributes = {}; this.morphTargetsRelative = false; this.groups = [];
		this.boundingBox = null; this.boundingSphere = null; this.drawRange = { start: 0, count: Infinity }; this.userData = {};
	}
	getIndex() { return this.index; }
	setIndex(index) {
		if (Array.isArray(index)) this.index = new (arrayMax(index) > 65535 ? Uint32BufferAttribute : Uint16BufferAttribute)(index, 1);
		else this.index = index;
		return this;
	}
	getAttribute(name) { return this.attributes[name]; }
	setAttribute(name, attribute) { this.attributes[name] = attribute; return this; }
	deleteAttribute(name) { delete this.attributes[name]; return this; }
	hasAttribute(name) { return this.attributes[name] !== undefined; }
	addGroup(start, count, materialIndex = 0) { this.groups.push({ start, count, materialIndex }); }
	clearGroups() { this.groups = []; }
	setDrawRange(start, count) { this.drawRange.start = start; this.drawRange.count = count; }
	applyMatrix4(matrix) {
		const position = this.attributes.position;
		if (position !== undefined) { position.applyMatrix4(matrix); position.needsUpdate = true; }
		const normal = this.attributes.normal;
		if (normal !== undefined) { const normalMatrix = new Matrix3().getNormalMatrix(matrix); normal.applyNormalMatrix(normalMatrix); normal.needsUpdate = true; }
		const tangent = this.attributes.tangent;
		if (tangent !== undefined) { tangent.transformDirection(matrix); tangent.needsUpdate = true; }
		return this;
	}
	rotateX(angle) { _m1.makeRotationX(angle); this.applyMatrix4(_m1); return this; }
	rotateY(angle) { _m1.makeRotationY(angle); this.applyMatrix4(_m1); return this; }
	rotateZ(angle) { _m1.makeRotationZ(angle); this.applyMatrix4(_m1); return this; }
	translate(x, y, z) { _m1.makeTranslation(x, y, z); this.applyMatrix4(_m1); return this; }
	scale(x, y, z) { _m1.makeScale(x, y, z); this.applyMatrix4(_m1); return this; }
	setFromPoints(points) {
		const position = [];
		for (const p of points) position.push(p.x, p.y, p.z || 0);
		this.setAttribute('position', new Float32BufferAttribute(position, 3));
		return this;
	}
	computeVertexNormals() {
		const index = this.index, positionAttribute = this.getAttribute('position');
		if (positionAttribute === undefined) return;
		let normalAttribute = this.getAttribute('normal');
		if (normalAttribute === undefined) { normalAttribute = new BufferAttribute(new Float32Array(positionAttribute.count * 3), 3); this.setAttribute('normal', normalAttribute); }
		else for (let i = 0, il = normalAttribute.count; i < il; i++) normalAttribute.setXYZ(i, 0, 0, 0);
		const pA = new Vector3(), pB = new Vector3(), pC = new Vector3(), nA = new Vector3(), nB = new Vector3(), nC = new Vector3(), cb = new Vector3(), ab = new Vector3();
		if (index) {
			for (let i = 0, il = index.count; i < il; i += 3) {
				const vA = index.getX(i), vB = index.getX(i + 1), vC = index.getX(i + 2);
				pA.fromBufferAttribute(positionAttribute, vA); pB.fromBufferAttribute(positionAttribute, vB); pC.fromBufferAttribute(positionAttribute, vC);
				cb.subVectors(pC, pB); ab.subVectors(pA, pB); cb.cross(ab);
				nA.fromBufferAttribute(normalAttribute, vA); nB.fromBufferAttribute(normalAttribute, vB); nC.fromBufferAttribute(normalAttribute, vC);
				nA.add(cb); nB.add(cb); nC.add(cb);
				normalAttribute.setXYZ(vA, nA.x, nA.y, nA.z); normalAttribute.setXYZ(vB, nB.x, nB.y, nB.z); normalAttribute.setXYZ(vC, nC.x, nC.y, nC.z);
			}
		} else {
			for (let i = 0, il = positionAttribute.count; i < il; i += 3) {
				pA.fromBufferAttribute(positionAttribute, i); pB.fromBufferAttribute(positionAttribute, i + 1); pC.fromBufferAttribute(positionAttribute, i + 2);
				cb.subVectors(pC, pB); ab.subVectors(pA, pB); cb.cross(ab);
				normalAttribute.setXYZ(i, cb.x, cb.y, cb.z); normalAttribute.setXYZ(i + 1, cb.x, cb.y, cb.z); normalAttribute.setXYZ(i + 2, cb.x, cb.y, cb.z);
			}
		}
		this.normalizeNormals();
		normalAttribute.needsUpdate = true;
	}
	normalizeNormals() {
		const normals = this.attributes.normal;
		for (let i = 0, il = normals.count; i < il; i++) { _vectorA.fromBufferAttribute(normals, i); _vectorA.normalize(); normals.setXYZ(i, _vectorA.x, _vectorA.y, _vectorA.z); }
	}
	toNonIndexed() {
		function convertBufferAttribute(attribute, indices) {
			const array = attribute.array, itemSize = attribute.itemSize, normalized = attribute.normalized;
			const array2 = new array.constructor(indices.length * itemSize);
			let index = 0, index2 = 0;
			for (let i = 0, l = indices.length; i < l; i++) { index = indices[i] * itemSize; for (let j = 0; j < itemSize; j++) array2[index2++] = array[index++]; }
			return new BufferAttribute(array2, itemSize, normalized);
		}
		if (this.index === null) { console.warn('THREE.BufferGeometry.toNonIndexed(): Geometry is already non-indexed.'); return this; }
		const geometry2 = new BufferGeometry(), indices = this.index.array, attributes = this.attributes;
		for (const name in attributes) geometry2.setAttribute(name, convertBufferAttribute(attributes[name], indices));
		for (const group of this.groups) geometry2.addGroup(group.start, group.count, group.materialIndex);
		return geometry2;
	}
	copy(source) {
		this.index = null; this.attributes = {}; this.morphAttributes = {}; this.groups = [];
		this.name = source.name;
		if (source.index !== null) this.setIndex(source.index.clone());
		for (const name in source.attributes) this.setAttribute(name, source.attributes[name].clone());
		for (const g of source.groups) this.addGroup(g.start, g.count, g.materialIndex);
		this.drawRange.start = source.drawRange.start; this.drawRange.count = source.drawRange.count;
		this.userData = source.userData;
		return this;
	}
	clone() { return new BufferGeometry().copy(this); }
	dispose() {}
}
BufferGeometry.prototype.isBufferGeometry = true;

// ---------------------------------------------------------------- the basic geometries
class BoxGeometry extends BufferGeometry {
	constructor(width = 1, height = 1, depth = 1, widthSegments = 1, heightSegments = 1, depthSegments = 1) {
		super();
		this.type = 'BoxGeometry';
		this.parameters = { width, height, depth, widthSegments, heightSegments, depthSegments };
		const scope = this;
		widthSegments = Math.floor(widthSegments); heightSegments = Math.floor(heightSegments); depthSegments = Math.floor(depthSegments);
		const indices = [], vertices = [], normals = [], uvs = [];
		let numberOfVertices = 0, groupStart = 0;
		buildPlane('z', 'y', 'x', -1, -1, depth, height, width, depthSegments, heightSegments, 0); // px
		buildPlane('z', 'y', 'x', 1, -1, depth, height, -width, depthSegments, heightSegments, 1); // nx
		buildPlane('x', 'z', 'y', 1, 1, width, depth, height, widthSegments, depthSegments, 2); // py
		buildPlane('x', 'z', 'y', 1, -1, width, depth, -height, widthSegments, depthSegments, 3); // ny
		buildPlane('x', 'y', 'z', 1, -1, width, height, depth, widthSegments, heightSegments, 4); // pz
		buildPlane('x', 'y', 'z', -1, -1, width, height, -depth, widthSegments, heightSegments, 5); // nz
		this.setIndex(indices);
		this.setAttribute('position', new Float32BufferAttribute(vertices, 3));
		this.setAttribute('normal', new Float32BufferAttribute(normals, 3));
		this.setAttribute('uv', new Float32BufferAttribute(uvs, 2));
		function buildPlane(u, v, w, udir, vdir, width, height, depth, gridX, gridY, materialIndex) {
			const segmentWidth = width / gridX, segmentHeight = height / gridY, widthHalf = width / 2, heightHalf = height / 2, depthHalf = depth / 2;
			const gridX1 = gridX + 1, gridY1 = gridY + 1;
			let vertexCounter = 0, groupCount = 0;
			const vector = new Vector3();
			for (let iy = 0; iy < gridY1; iy++) {
				const y = iy * segmentHeight - heightHalf;
				for (let ix = 0; ix < gridX1; ix++) {
					const x = ix * segmentWidth - widthHalf;
					vector[u] = x * udir; vector[v] = y * vdir; vector[w] = depthHalf;
					vertices.push(vector.x, vector.y, vector.z);
					vector[u] = 0; vector[v] = 0; vector[w] = depth > 0 ? 1 : -1;
					normals.push(vector.x, vector.y, vector.z);
					uvs.push(ix / gridX); uvs.push(1 - (iy / gridY));
					vertexCounter += 1;
				}
			}
			for (let iy = 0; iy < gridY; iy++) {
				for (let ix = 0; ix < gridX; ix++) {
					const a = numberOfVertices + ix + gridX1 * iy;
					const b = numberOfVertices + ix + gridX1 * (iy + 1);
					const c = numberOfVertices + (ix + 1) + gridX1 * (iy + 1);
					const d = numberOfVertices + (ix + 1) + gridX1 * iy;
					indices.push(a, b, d); indices.push(b, c, d);
					groupCount += 6;
				}
			}
			scope.addGroup(groupStart, groupCount, materialIndex);
			groupStart += groupCount; numberOfVertices += vertexCounter;
		}
	}
}

class CylinderGeometry extends BufferGeometry {
	constructor(radiusTop = 1, radiusBottom = 1, height = 1, radialSegments = 8, heightSegments = 1, openEnded = false, thetaStart = 0, thetaLength = Math.PI * 2) {
		super();
		this.type = 'CylinderGeometry';
		this.parameters = { radiusTop, radiusBottom, height, radialSegments, heightSegments, openEnded, thetaStart, thetaLength };
		const scope = this;
		radialSegments = Math.floor(radialSegments); heightSegments = Math.floor(heightSegments);
		const indices = [], vertices = [], normals = [], uvs = [];
		let index = 0, groupStart = 0;
		const indexArray = [], halfHeight = height / 2;
		generateTorso();
		if (openEnded === false) { if (radiusTop > 0) generateCap(true); if (radiusBottom > 0) generateCap(false); }
		this.setIndex(indices);
		this.setAttribute('position', new Float32BufferAttribute(vertices, 3));
		this.setAttribute('normal', new Float32BufferAttribute(normals, 3));
		this.setAttribute('uv', new Float32BufferAttribute(uvs, 2));
		function generateTorso() {
			const normal = new Vector3(), vertex = new Vector3();
			let groupCount = 0;
			const slope = (radiusBottom - radiusTop) / height;
			for (let y = 0; y <= heightSegments; y++) {
				const indexRow = [], v = y / heightSegments, radius = v * (radiusBottom - radiusTop) + radiusTop;
				for (let x = 0; x <= radialSegments; x++) {
					const u = x / radialSegments, theta = u * thetaLength + thetaStart, sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);
					vertex.x = radius * sinTheta; vertex.y = -v * height + halfHeight; vertex.z = radius * cosTheta;
					vertices.push(vertex.x, vertex.y, vertex.z);
					normal.set(sinTheta, slope, cosTheta).normalize();
					normals.push(normal.x, normal.y, normal.z);
					uvs.push(u, 1 - v);
					indexRow.push(index++);
				}
				indexArray.push(indexRow);
			}
			for (let x = 0; x < radialSegments; x++) {
				for (let y = 0; y < heightSegments; y++) {
					const a = indexArray[y][x], b = indexArray[y + 1][x], c = indexArray[y + 1][x + 1], d = indexArray[y][x + 1];
					indices.push(a, b, d); indices.push(b, c, d);
					groupCount += 6;
				}
			}
			scope.addGroup(groupStart, groupCount, 0);
			groupStart += groupCount;
		}
		function generateCap(top) {
			const centerIndexStart = index, uv = new Vector2(), vertex = new Vector3();
			let groupCount = 0;
			const radius = (top === true) ? radiusTop : radiusBottom, sign = (top === true) ? 1 : -1;
			for (let x = 1; x <= radialSegments; x++) { vertices.push(0, halfHeight * sign, 0); normals.push(0, sign, 0); uvs.push(0.5, 0.5); index++; }
			const centerIndexEnd = index;
			for (let x = 0; x <= radialSegments; x++) {
				const u = x / radialSegments, theta = u * thetaLength + thetaStart, cosTheta = Math.cos(theta), sinTheta = Math.sin(theta);
				vertex.x = radius * sinTheta; vertex.y = halfHeight * sign; vertex.z = radius * cosTheta;
				vertices.push(vertex.x, vertex.y, vertex.z);
				normals.push(0, sign, 0);
				uv.x = (cosTheta * 0.5) + 0.5; uv.y = (sinTheta * 0.5 * sign) + 0.5;
				uvs.push(uv.x, uv.y);
				index++;
			}
			for (let x = 0; x < radialSegments; x++) {
				const c = centerIndexStart + x, i = centerIndexEnd + x;
				if (top === true) indices.push(i, i + 1, c); else indices.push(i + 1, i, c);
				groupCount += 3;
			}
			scope.addGroup(groupStart, groupCount, top === true ? 1 : 2);
			groupStart += groupCount;
		}
	}
}

class SphereGeometry extends BufferGeometry {
	constructor(radius = 1, widthSegments = 8, heightSegments = 6, phiStart = 0, phiLength = Math.PI * 2, thetaStart = 0, thetaLength = Math.PI) {
		super();
		this.type = 'SphereGeometry';
		this.parameters = { radius, widthSegments, heightSegments, phiStart, phiLength, thetaStart, thetaLength };
		widthSegments = Math.max(3, Math.floor(widthSegments)); heightSegments = Math.max(2, Math.floor(heightSegments));
		const thetaEnd = Math.min(thetaStart + thetaLength, Math.PI);
		let index = 0;
		const grid = [], vertex = new Vector3(), normal = new Vector3(), indices = [], vertices = [], normals = [], uvs = [];
		for (let iy = 0; iy <= heightSegments; iy++) {
			const verticesRow = [], v = iy / heightSegments;
			let uOffset = 0;
			if (iy == 0 && thetaStart == 0) uOffset = 0.5 / widthSegments;
			else if (iy == heightSegments && thetaEnd == Math.PI) uOffset = -0.5 / widthSegments;
			for (let ix = 0; ix <= widthSegments; ix++) {
				const u = ix / widthSegments;
				vertex.x = -radius * Math.cos(phiStart + u * phiLength) * Math.sin(thetaStart + v * thetaLength);
				vertex.y = radius * Math.cos(thetaStart + v * thetaLength);
				vertex.z = radius * Math.sin(phiStart + u * phiLength) * Math.sin(thetaStart + v * thetaLength);
				vertices.push(vertex.x, vertex.y, vertex.z);
				normal.copy(vertex).normalize();
				normals.push(normal.x, normal.y, normal.z);
				uvs.push(u + uOffset, 1 - v);
				verticesRow.push(index++);
			}
			grid.push(verticesRow);
		}
		for (let iy = 0; iy < heightSegments; iy++) {
			for (let ix = 0; ix < widthSegments; ix++) {
				const a = grid[iy][ix + 1], b = grid[iy][ix], c = grid[iy + 1][ix], d = grid[iy + 1][ix + 1];
				if (iy !== 0 || thetaStart > 0) indices.push(a, b, d);
				if (iy !== heightSegments - 1 || thetaEnd < Math.PI) indices.push(b, c, d);
			}
		}
		this.setIndex(indices);
		this.setAttribute('position', new Float32BufferAttribute(vertices, 3));
		this.setAttribute('normal', new Float32BufferAttribute(normals, 3));
		this.setAttribute('uv', new Float32BufferAttribute(uvs, 2));
	}
}

class TorusGeometry extends BufferGeometry {
	constructor(radius = 1, tube = 0.4, radialSegments = 8, tubularSegments = 6, arc = Math.PI * 2) {
		super();
		this.type = 'TorusGeometry';
		this.parameters = { radius, tube, radialSegments, tubularSegments, arc };
		radialSegments = Math.floor(radialSegments); tubularSegments = Math.floor(tubularSegments);
		const indices = [], vertices = [], normals = [], uvs = [], center = new Vector3(), vertex = new Vector3(), normal = new Vector3();
		for (let j = 0; j <= radialSegments; j++) {
			for (let i = 0; i <= tubularSegments; i++) {
				const u = i / tubularSegments * arc, v = j / radialSegments * Math.PI * 2;
				vertex.x = (radius + tube * Math.cos(v)) * Math.cos(u);
				vertex.y = (radius + tube * Math.cos(v)) * Math.sin(u);
				vertex.z = tube * Math.sin(v);
				vertices.push(vertex.x, vertex.y, vertex.z);
				center.x = radius * Math.cos(u); center.y = radius * Math.sin(u);
				normal.subVectors(vertex, center).normalize();
				normals.push(normal.x, normal.y, normal.z);
				uvs.push(i / tubularSegments); uvs.push(j / radialSegments);
			}
		}
		for (let j = 1; j <= radialSegments; j++) {
			for (let i = 1; i <= tubularSegments; i++) {
				const a = (tubularSegments + 1) * j + i - 1, b = (tubularSegments + 1) * (j - 1) + i - 1, c = (tubularSegments + 1) * (j - 1) + i, d = (tubularSegments + 1) * j + i;
				indices.push(a, b, d); indices.push(b, c, d);
			}
		}
		this.setIndex(indices);
		this.setAttribute('position', new Float32BufferAttribute(vertices, 3));
		this.setAttribute('normal', new Float32BufferAttribute(normals, 3));
		this.setAttribute('uv', new Float32BufferAttribute(uvs, 2));
	}
}

class LatheGeometry extends BufferGeometry {
	constructor(points, segments = 12, phiStart = 0, phiLength = Math.PI * 2) {
		super();
		this.type = 'LatheGeometry';
		this.parameters = { points, segments, phiStart, phiLength };
		segments = Math.floor(segments);
		phiLength = clamp(phiLength, 0, Math.PI * 2);
		const indices = [], vertices = [], uvs = [], inverseSegments = 1.0 / segments, vertex = new Vector3(), uv = new Vector2();
		for (let i = 0; i <= segments; i++) {
			const phi = phiStart + i * inverseSegments * phiLength, sin = Math.sin(phi), cos = Math.cos(phi);
			for (let j = 0; j <= (points.length - 1); j++) {
				vertex.x = points[j].x * sin; vertex.y = points[j].y; vertex.z = points[j].x * cos;
				vertices.push(vertex.x, vertex.y, vertex.z);
				uv.x = i / segments; uv.y = j / (points.length - 1);
				uvs.push(uv.x, uv.y);
			}
		}
		for (let i = 0; i < segments; i++) {
			for (let j = 0; j < (points.length - 1); j++) {
				const base = j + i * points.length, a = base, b = base + points.length, c = base + points.length + 1, d = base + 1;
				indices.push(a, b, d); indices.push(b, c, d);
			}
		}
		this.setIndex(indices);
		this.setAttribute('position', new Float32BufferAttribute(vertices, 3));
		this.setAttribute('uv', new Float32BufferAttribute(uvs, 2));
		this.computeVertexNormals();
	}
}

class PlaneGeometry extends BufferGeometry {
	constructor(width = 1, height = 1, widthSegments = 1, heightSegments = 1) {
		super();
		this.type = 'PlaneGeometry';
		this.parameters = { width, height, widthSegments, heightSegments };
		const width_half = width / 2, height_half = height / 2, gridX = Math.floor(widthSegments), gridY = Math.floor(heightSegments);
		const gridX1 = gridX + 1, gridY1 = gridY + 1, segment_width = width / gridX, segment_height = height / gridY;
		const indices = [], vertices = [], normals = [], uvs = [];
		for (let iy = 0; iy < gridY1; iy++) {
			const y = iy * segment_height - height_half;
			for (let ix = 0; ix < gridX1; ix++) {
				const x = ix * segment_width - width_half;
				vertices.push(x, -y, 0); normals.push(0, 0, 1);
				uvs.push(ix / gridX); uvs.push(1 - (iy / gridY));
			}
		}
		for (let iy = 0; iy < gridY; iy++) {
			for (let ix = 0; ix < gridX; ix++) {
				const a = ix + gridX1 * iy, b = ix + gridX1 * (iy + 1), c = (ix + 1) + gridX1 * (iy + 1), d = (ix + 1) + gridX1 * iy;
				indices.push(a, b, d); indices.push(b, c, d);
			}
		}
		this.setIndex(indices);
		this.setAttribute('position', new Float32BufferAttribute(vertices, 3));
		this.setAttribute('normal', new Float32BufferAttribute(normals, 3));
		this.setAttribute('uv', new Float32BufferAttribute(uvs, 2));
	}
}

class CircleGeometry extends BufferGeometry {
	constructor(radius = 1, segments = 8, thetaStart = 0, thetaLength = Math.PI * 2) {
		super();
		this.type = 'CircleGeometry';
		this.parameters = { radius, segments, thetaStart, thetaLength };
		segments = Math.max(3, segments);
		const indices = [], vertices = [], normals = [], uvs = [], vertex = new Vector3(), uv = new Vector2();
		vertices.push(0, 0, 0); normals.push(0, 0, 1); uvs.push(0.5, 0.5);
		for (let s = 0, i = 3; s <= segments; s++, i += 3) {
			const segment = thetaStart + s / segments * thetaLength;
			vertex.x = radius * Math.cos(segment); vertex.y = radius * Math.sin(segment);
			vertices.push(vertex.x, vertex.y, vertex.z); normals.push(0, 0, 1);
			uv.x = (vertices[i] / radius + 1) / 2; uv.y = (vertices[i + 1] / radius + 1) / 2;
			uvs.push(uv.x, uv.y);
		}
		for (let i = 1; i <= segments; i++) indices.push(i, i + 1, 0);
		this.setIndex(indices);
		this.setAttribute('position', new Float32BufferAttribute(vertices, 3));
		this.setAttribute('normal', new Float32BufferAttribute(normals, 3));
		this.setAttribute('uv', new Float32BufferAttribute(uvs, 2));
	}
}

class RingGeometry extends BufferGeometry {
	constructor(innerRadius = 0.5, outerRadius = 1, thetaSegments = 8, phiSegments = 1, thetaStart = 0, thetaLength = Math.PI * 2) {
		super();
		this.type = 'RingGeometry';
		this.parameters = { innerRadius, outerRadius, thetaSegments, phiSegments, thetaStart, thetaLength };
		thetaSegments = Math.max(3, thetaSegments); phiSegments = Math.max(1, phiSegments);
		const indices = [], vertices = [], normals = [], uvs = [];
		let radius = innerRadius;
		const radiusStep = ((outerRadius - innerRadius) / phiSegments), vertex = new Vector3(), uv = new Vector2();
		for (let j = 0; j <= phiSegments; j++) {
			for (let i = 0; i <= thetaSegments; i++) {
				const segment = thetaStart + i / thetaSegments * thetaLength;
				vertex.x = radius * Math.cos(segment); vertex.y = radius * Math.sin(segment);
				vertices.push(vertex.x, vertex.y, vertex.z); normals.push(0, 0, 1);
				uv.x = (vertex.x / outerRadius + 1) / 2; uv.y = (vertex.y / outerRadius + 1) / 2;
				uvs.push(uv.x, uv.y);
			}
			radius += radiusStep;
		}
		for (let j = 0; j < phiSegments; j++) {
			const thetaSegmentLevel = j * (thetaSegments + 1);
			for (let i = 0; i < thetaSegments; i++) {
				const segment = i + thetaSegmentLevel, a = segment, b = segment + thetaSegments + 1, c = segment + thetaSegments + 2, d = segment + 1;
				indices.push(a, b, d); indices.push(b, c, d);
			}
		}
		this.setIndex(indices);
		this.setAttribute('position', new Float32BufferAttribute(vertices, 3));
		this.setAttribute('normal', new Float32BufferAttribute(normals, 3));
		this.setAttribute('uv', new Float32BufferAttribute(uvs, 2));
	}
}

// ---------------------------------------------------------------- curves, paths and shapes
class Curve {
	constructor() { this.type = 'Curve'; this.arcLengthDivisions = 200; }
	getPoint() { console.warn('THREE.Curve: .getPoint() not implemented.'); return null; }
	getPoints(divisions = 5) { const points = []; for (let d = 0; d <= divisions; d++) points.push(this.getPoint(d / divisions)); return points; }
}
class EllipseCurve extends Curve {
	constructor(aX = 0, aY = 0, xRadius = 1, yRadius = 1, aStartAngle = 0, aEndAngle = Math.PI * 2, aClockwise = false, aRotation = 0) {
		super();
		this.type = 'EllipseCurve';
		this.aX = aX; this.aY = aY; this.xRadius = xRadius; this.yRadius = yRadius;
		this.aStartAngle = aStartAngle; this.aEndAngle = aEndAngle; this.aClockwise = aClockwise; this.aRotation = aRotation;
	}
	getPoint(t, optionalTarget) {
		const point = optionalTarget || new Vector2(), twoPi = Math.PI * 2;
		let deltaAngle = this.aEndAngle - this.aStartAngle;
		const samePoints = Math.abs(deltaAngle) < Number.EPSILON;
		while (deltaAngle < 0) deltaAngle += twoPi;
		while (deltaAngle > twoPi) deltaAngle -= twoPi;
		if (deltaAngle < Number.EPSILON) deltaAngle = samePoints ? 0 : twoPi;
		if (this.aClockwise === true && !samePoints) deltaAngle = (deltaAngle === twoPi) ? -twoPi : deltaAngle - twoPi;
		const angle = this.aStartAngle + t * deltaAngle;
		let x = this.aX + this.xRadius * Math.cos(angle), y = this.aY + this.yRadius * Math.sin(angle);
		if (this.aRotation !== 0) {
			const cos = Math.cos(this.aRotation), sin = Math.sin(this.aRotation), tx = x - this.aX, ty = y - this.aY;
			x = tx * cos - ty * sin + this.aX; y = tx * sin + ty * cos + this.aY;
		}
		return point.set(x, y);
	}
}
EllipseCurve.prototype.isEllipseCurve = true;
class ArcCurve extends EllipseCurve { constructor(aX, aY, aRadius, aStartAngle, aEndAngle, aClockwise) { super(aX, aY, aRadius, aRadius, aStartAngle, aEndAngle, aClockwise); this.type = 'ArcCurve'; } }
ArcCurve.prototype.isArcCurve = true;
class LineCurve extends Curve {
	constructor(v1 = new Vector2(), v2 = new Vector2()) { super(); this.type = 'LineCurve'; this.v1 = v1; this.v2 = v2; }
	getPoint(t, optionalTarget = new Vector2()) {
		const point = optionalTarget;
		if (t === 1) point.copy(this.v2);
		else { point.copy(this.v2).sub(this.v1); point.multiplyScalar(t).add(this.v1); }
		return point;
	}
}
LineCurve.prototype.isLineCurve = true;
function QuadraticBezier(t, p0, p1, p2) { const k = 1 - t; return k * k * p0 + 2 * (1 - t) * t * p1 + t * t * p2; }
function CubicBezier(t, p0, p1, p2, p3) { const k = 1 - t; return k * k * k * p0 + 3 * k * k * t * p1 + 3 * k * t * t * p2 + t * t * t * p3; }
class QuadraticBezierCurve extends Curve {
	constructor(v0 = new Vector2(), v1 = new Vector2(), v2 = new Vector2()) { super(); this.type = 'QuadraticBezierCurve'; this.v0 = v0; this.v1 = v1; this.v2 = v2; }
	getPoint(t, optionalTarget = new Vector2()) { return optionalTarget.set(QuadraticBezier(t, this.v0.x, this.v1.x, this.v2.x), QuadraticBezier(t, this.v0.y, this.v1.y, this.v2.y)); }
}
class CubicBezierCurve extends Curve {
	constructor(v0 = new Vector2(), v1 = new Vector2(), v2 = new Vector2(), v3 = new Vector2()) { super(); this.type = 'CubicBezierCurve'; this.v0 = v0; this.v1 = v1; this.v2 = v2; this.v3 = v3; }
	getPoint(t, optionalTarget = new Vector2()) { return optionalTarget.set(CubicBezier(t, this.v0.x, this.v1.x, this.v2.x, this.v3.x), CubicBezier(t, this.v0.y, this.v1.y, this.v2.y, this.v3.y)); }
}
class CurvePath extends Curve {
	constructor() { super(); this.type = 'CurvePath'; this.curves = []; this.autoClose = false; }
	add(curve) { this.curves.push(curve); }
	closePath() {
		const startPoint = this.curves[0].getPoint(0), endPoint = this.curves[this.curves.length - 1].getPoint(1);
		if (!startPoint.equals(endPoint)) this.curves.push(new LineCurve(endPoint, startPoint));
	}
	getPoints(divisions = 12) {
		const points = [];
		let last;
		for (let i = 0, curves = this.curves; i < curves.length; i++) {
			const curve = curves[i];
			const resolution = (curve && curve.isEllipseCurve) ? divisions * 2 : (curve && curve.isLineCurve) ? 1 : divisions;
			const pts = curve.getPoints(resolution);
			for (let j = 0; j < pts.length; j++) {
				const point = pts[j];
				if (last && last.equals(point)) continue;
				points.push(point);
				last = point;
			}
		}
		if (this.autoClose && points.length > 1 && !points[points.length - 1].equals(points[0])) points.push(points[0]);
		return points;
	}
}
class Path extends CurvePath {
	constructor(points) { super(); this.type = 'Path'; this.currentPoint = new Vector2(); if (points) this.setFromPoints(points); }
	setFromPoints(points) { this.moveTo(points[0].x, points[0].y); for (let i = 1, l = points.length; i < l; i++) this.lineTo(points[i].x, points[i].y); return this; }
	moveTo(x, y) { this.currentPoint.set(x, y); return this; }
	lineTo(x, y) { const curve = new LineCurve(this.currentPoint.clone(), new Vector2(x, y)); this.curves.push(curve); this.currentPoint.set(x, y); return this; }
	quadraticCurveTo(aCPx, aCPy, aX, aY) { this.curves.push(new QuadraticBezierCurve(this.currentPoint.clone(), new Vector2(aCPx, aCPy), new Vector2(aX, aY))); this.currentPoint.set(aX, aY); return this; }
	bezierCurveTo(aCP1x, aCP1y, aCP2x, aCP2y, aX, aY) { this.curves.push(new CubicBezierCurve(this.currentPoint.clone(), new Vector2(aCP1x, aCP1y), new Vector2(aCP2x, aCP2y), new Vector2(aX, aY))); this.currentPoint.set(aX, aY); return this; }
	arc(aX, aY, aRadius, aStartAngle, aEndAngle, aClockwise) { const x0 = this.currentPoint.x, y0 = this.currentPoint.y; this.absarc(aX + x0, aY + y0, aRadius, aStartAngle, aEndAngle, aClockwise); return this; }
	absarc(aX, aY, aRadius, aStartAngle, aEndAngle, aClockwise) { this.absellipse(aX, aY, aRadius, aRadius, aStartAngle, aEndAngle, aClockwise); return this; }
	ellipse(aX, aY, xRadius, yRadius, aStartAngle, aEndAngle, aClockwise, aRotation) { const x0 = this.currentPoint.x, y0 = this.currentPoint.y; this.absellipse(aX + x0, aY + y0, xRadius, yRadius, aStartAngle, aEndAngle, aClockwise, aRotation); return this; }
	absellipse(aX, aY, xRadius, yRadius, aStartAngle, aEndAngle, aClockwise, aRotation) {
		const curve = new EllipseCurve(aX, aY, xRadius, yRadius, aStartAngle, aEndAngle, aClockwise, aRotation);
		if (this.curves.length > 0) { const firstPoint = curve.getPoint(0); if (!firstPoint.equals(this.currentPoint)) this.lineTo(firstPoint.x, firstPoint.y); }
		this.curves.push(curve);
		const lastPoint = curve.getPoint(1);
		this.currentPoint.copy(lastPoint);
		return this;
	}
}
class Shape extends Path {
	constructor(points) { super(points); this.uuid = generateUUID(); this.type = 'Shape'; this.holes = []; }
	getPointsHoles(divisions) { const holesPts = []; for (let i = 0, l = this.holes.length; i < l; i++) holesPts[i] = this.holes[i].getPoints(divisions); return holesPts; }
	extractPoints(divisions) { return { shape: this.getPoints(divisions), holes: this.getPointsHoles(divisions) }; }
}

// ---------------------------------------------------------------- earcut 2.2 (as ported in three r128)
const Earcut = {
	triangulate(data, holeIndices, dim) {
		dim = dim || 2;
		const hasHoles = holeIndices && holeIndices.length, outerLen = hasHoles ? holeIndices[0] * dim : data.length;
		let outerNode = linkedList(data, 0, outerLen, dim, true);
		const triangles = [];
		if (!outerNode || outerNode.next === outerNode.prev) return triangles;
		let minX, minY, maxX, maxY, x, y, invSize;
		if (hasHoles) outerNode = eliminateHoles(data, holeIndices, outerNode, dim);
		if (data.length > 80 * dim) {
			minX = maxX = data[0]; minY = maxY = data[1];
			for (let i = dim; i < outerLen; i += dim) {
				x = data[i]; y = data[i + 1];
				if (x < minX) minX = x; if (y < minY) minY = y; if (x > maxX) maxX = x; if (y > maxY) maxY = y;
			}
			invSize = Math.max(maxX - minX, maxY - minY);
			invSize = invSize !== 0 ? 1 / invSize : 0;
		}
		earcutLinked(outerNode, triangles, dim, minX, minY, invSize);
		return triangles;
	}
};
function linkedList(data, start, end, dim, clockwise) {
	let i, last;
	if (clockwise === (signedArea(data, start, end, dim) > 0)) { for (i = start; i < end; i += dim) last = insertNode(i, data[i], data[i + 1], last); }
	else { for (i = end - dim; i >= start; i -= dim) last = insertNode(i, data[i], data[i + 1], last); }
	if (last && equals(last, last.next)) { removeNode(last); last = last.next; }
	return last;
}
function filterPoints(start, end) {
	if (!start) return start;
	if (!end) end = start;
	let p = start, again;
	do {
		again = false;
		if (!p.steiner && (equals(p, p.next) || area(p.prev, p, p.next) === 0)) {
			removeNode(p);
			p = end = p.prev;
			if (p === p.next) break;
			again = true;
		} else p = p.next;
	} while (again || p !== end);
	return end;
}
function earcutLinked(ear, triangles, dim, minX, minY, invSize, pass) {
	if (!ear) return;
	if (!pass && invSize) indexCurve(ear, minX, minY, invSize);
	let stop = ear, prev, next;
	while (ear.prev !== ear.next) {
		prev = ear.prev; next = ear.next;
		if (invSize ? isEarHashed(ear, minX, minY, invSize) : isEar(ear)) {
			triangles.push(prev.i / dim); triangles.push(ear.i / dim); triangles.push(next.i / dim);
			removeNode(ear);
			ear = next.next; stop = next.next;
			continue;
		}
		ear = next;
		if (ear === stop) {
			if (!pass) earcutLinked(filterPoints(ear), triangles, dim, minX, minY, invSize, 1);
			else if (pass === 1) { ear = cureLocalIntersections(filterPoints(ear), triangles, dim); earcutLinked(ear, triangles, dim, minX, minY, invSize, 2); }
			else if (pass === 2) splitEarcut(ear, triangles, dim, minX, minY, invSize);
			break;
		}
	}
}
function isEar(ear) {
	const a = ear.prev, b = ear, c = ear.next;
	if (area(a, b, c) >= 0) return false;
	let p = ear.next.next;
	while (p !== ear.prev) {
		if (pointInTriangle(a.x, a.y, b.x, b.y, c.x, c.y, p.x, p.y) && area(p.prev, p, p.next) >= 0) return false;
		p = p.next;
	}
	return true;
}
function isEarHashed(ear, minX, minY, invSize) {
	const a = ear.prev, b = ear, c = ear.next;
	if (area(a, b, c) >= 0) return false;
	const minTX = a.x < b.x ? (a.x < c.x ? a.x : c.x) : (b.x < c.x ? b.x : c.x), minTY = a.y < b.y ? (a.y < c.y ? a.y : c.y) : (b.y < c.y ? b.y : c.y);
	const maxTX = a.x > b.x ? (a.x > c.x ? a.x : c.x) : (b.x > c.x ? b.x : c.x), maxTY = a.y > b.y ? (a.y > c.y ? a.y : c.y) : (b.y > c.y ? b.y : c.y);
	const minZ = zOrder(minTX, minTY, minX, minY, invSize), maxZ = zOrder(maxTX, maxTY, minX, minY, invSize);
	let p = ear.prevZ, n = ear.nextZ;
	while (p && p.z >= minZ && n && n.z <= maxZ) {
		if (p !== ear.prev && p !== ear.next && pointInTriangle(a.x, a.y, b.x, b.y, c.x, c.y, p.x, p.y) && area(p.prev, p, p.next) >= 0) return false;
		p = p.prevZ;
		if (n !== ear.prev && n !== ear.next && pointInTriangle(a.x, a.y, b.x, b.y, c.x, c.y, n.x, n.y) && area(n.prev, n, n.next) >= 0) return false;
		n = n.nextZ;
	}
	while (p && p.z >= minZ) {
		if (p !== ear.prev && p !== ear.next && pointInTriangle(a.x, a.y, b.x, b.y, c.x, c.y, p.x, p.y) && area(p.prev, p, p.next) >= 0) return false;
		p = p.prevZ;
	}
	while (n && n.z <= maxZ) {
		if (n !== ear.prev && n !== ear.next && pointInTriangle(a.x, a.y, b.x, b.y, c.x, c.y, n.x, n.y) && area(n.prev, n, n.next) >= 0) return false;
		n = n.nextZ;
	}
	return true;
}
function cureLocalIntersections(start, triangles, dim) {
	let p = start;
	do {
		const a = p.prev, b = p.next.next;
		if (!equals(a, b) && intersects(a, p, p.next, b) && locallyInside(a, b) && locallyInside(b, a)) {
			triangles.push(a.i / dim); triangles.push(p.i / dim); triangles.push(b.i / dim);
			removeNode(p); removeNode(p.next);
			p = start = b;
		}
		p = p.next;
	} while (p !== start);
	return filterPoints(p);
}
function splitEarcut(start, triangles, dim, minX, minY, invSize) {
	let a = start;
	do {
		let b = a.next.next;
		while (b !== a.prev) {
			if (a.i !== b.i && isValidDiagonal(a, b)) {
				let c = splitPolygon(a, b);
				a = filterPoints(a, a.next); c = filterPoints(c, c.next);
				earcutLinked(a, triangles, dim, minX, minY, invSize);
				earcutLinked(c, triangles, dim, minX, minY, invSize);
				return;
			}
			b = b.next;
		}
		a = a.next;
	} while (a !== start);
}
function eliminateHoles(data, holeIndices, outerNode, dim) {
	const queue = [];
	let i, len, start, end, list;
	for (i = 0, len = holeIndices.length; i < len; i++) {
		start = holeIndices[i] * dim;
		end = i < len - 1 ? holeIndices[i + 1] * dim : data.length;
		list = linkedList(data, start, end, dim, false);
		if (list === list.next) list.steiner = true;
		queue.push(getLeftmost(list));
	}
	queue.sort(compareX);
	for (i = 0; i < queue.length; i++) { eliminateHole(queue[i], outerNode); outerNode = filterPoints(outerNode, outerNode.next); }
	return outerNode;
}
function compareX(a, b) { return a.x - b.x; }
function eliminateHole(hole, outerNode) {
	outerNode = findHoleBridge(hole, outerNode);
	if (outerNode) { const b = splitPolygon(outerNode, hole); filterPoints(outerNode, outerNode.next); filterPoints(b, b.next); }
}
function findHoleBridge(hole, outerNode) {
	let p = outerNode;
	const hx = hole.x, hy = hole.y;
	let qx = -Infinity, m;
	do {
		if (hy <= p.y && hy >= p.next.y && p.next.y !== p.y) {
			const x = p.x + (hy - p.y) * (p.next.x - p.x) / (p.next.y - p.y);
			if (x <= hx && x > qx) {
				qx = x;
				if (x === hx) { if (hy === p.y) return p; if (hy === p.next.y) return p.next; }
				m = p.x < p.next.x ? p : p.next;
			}
		}
		p = p.next;
	} while (p !== outerNode);
	if (!m) return null;
	if (hx === qx) return m;
	const stop = m, mx = m.x, my = m.y;
	let tanMin = Infinity, tan;
	p = m;
	do {
		if (hx >= p.x && p.x >= mx && hx !== p.x && pointInTriangle(hy < my ? hx : qx, hy, mx, my, hy < my ? qx : hx, hy, p.x, p.y)) {
			tan = Math.abs(hy - p.y) / (hx - p.x);
			if (locallyInside(p, hole) && (tan < tanMin || (tan === tanMin && (p.x > m.x || (p.x === m.x && sectorContainsSector(m, p)))))) { m = p; tanMin = tan; }
		}
		p = p.next;
	} while (p !== stop);
	return m;
}
function sectorContainsSector(m, p) { return area(m.prev, m, p.prev) < 0 && area(p.next, m, m.next) < 0; }
function indexCurve(start, minX, minY, invSize) {
	let p = start;
	do {
		if (p.z === null) p.z = zOrder(p.x, p.y, minX, minY, invSize);
		p.prevZ = p.prev; p.nextZ = p.next; p = p.next;
	} while (p !== start);
	p.prevZ.nextZ = null; p.prevZ = null;
	sortLinked(p);
}
function sortLinked(list) {
	let i, p, q, e, tail, numMerges, pSize, qSize, inSize = 1;
	do {
		p = list; list = null; tail = null; numMerges = 0;
		while (p) {
			numMerges++;
			q = p; pSize = 0;
			for (i = 0; i < inSize; i++) { pSize++; q = q.nextZ; if (!q) break; }
			qSize = inSize;
			while (pSize > 0 || (qSize > 0 && q)) {
				if (pSize !== 0 && (qSize === 0 || !q || p.z <= q.z)) { e = p; p = p.nextZ; pSize--; }
				else { e = q; q = q.nextZ; qSize--; }
				if (tail) tail.nextZ = e; else list = e;
				e.prevZ = tail; tail = e;
			}
			p = q;
		}
		tail.nextZ = null;
		inSize *= 2;
	} while (numMerges > 1);
	return list;
}
function zOrder(x, y, minX, minY, invSize) {
	x = 32767 * (x - minX) * invSize; y = 32767 * (y - minY) * invSize;
	x = (x | (x << 8)) & 0x00FF00FF; x = (x | (x << 4)) & 0x0F0F0F0F; x = (x | (x << 2)) & 0x33333333; x = (x | (x << 1)) & 0x55555555;
	y = (y | (y << 8)) & 0x00FF00FF; y = (y | (y << 4)) & 0x0F0F0F0F; y = (y | (y << 2)) & 0x33333333; y = (y | (y << 1)) & 0x55555555;
	return x | (y << 1);
}
function getLeftmost(start) {
	let p = start, leftmost = start;
	do { if (p.x < leftmost.x || (p.x === leftmost.x && p.y < leftmost.y)) leftmost = p; p = p.next; } while (p !== start);
	return leftmost;
}
function pointInTriangle(ax, ay, bx, by, cx, cy, px, py) {
	return (cx - px) * (ay - py) - (ax - px) * (cy - py) >= 0 && (ax - px) * (by - py) - (bx - px) * (ay - py) >= 0 && (bx - px) * (cy - py) - (cx - px) * (by - py) >= 0;
}
function isValidDiagonal(a, b) {
	return a.next.i !== b.i && a.prev.i !== b.i && !intersectsPolygon(a, b) &&
		(locallyInside(a, b) && locallyInside(b, a) && middleInside(a, b) && (area(a.prev, a, b.prev) || area(a, b.prev, b)) ||
		equals(a, b) && area(a.prev, a, a.next) > 0 && area(b.prev, b, b.next) > 0);
}
function area(p, q, r) { return (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y); }
function equals(p1, p2) { return p1.x === p2.x && p1.y === p2.y; }
function intersects(p1, q1, p2, q2) {
	const o1 = sign(area(p1, q1, p2)), o2 = sign(area(p1, q1, q2)), o3 = sign(area(p2, q2, p1)), o4 = sign(area(p2, q2, q1));
	if (o1 !== o2 && o3 !== o4) return true;
	if (o1 === 0 && onSegment(p1, p2, q1)) return true;
	if (o2 === 0 && onSegment(p1, q2, q1)) return true;
	if (o3 === 0 && onSegment(p2, p1, q2)) return true;
	if (o4 === 0 && onSegment(p2, q1, q2)) return true;
	return false;
}
function onSegment(p, q, r) { return q.x <= Math.max(p.x, r.x) && q.x >= Math.min(p.x, r.x) && q.y <= Math.max(p.y, r.y) && q.y >= Math.min(p.y, r.y); }
function sign(num) { return num > 0 ? 1 : num < 0 ? -1 : 0; }
function intersectsPolygon(a, b) {
	let p = a;
	do {
		if (p.i !== a.i && p.next.i !== a.i && p.i !== b.i && p.next.i !== b.i && intersects(p, p.next, a, b)) return true;
		p = p.next;
	} while (p !== a);
	return false;
}
function locallyInside(a, b) {
	return area(a.prev, a, a.next) < 0 ? area(a, b, a.next) >= 0 && area(a, a.prev, b) >= 0 : area(a, b, a.prev) < 0 || area(a, a.next, b) < 0;
}
function middleInside(a, b) {
	let p = a, inside = false;
	const px = (a.x + b.x) / 2, py = (a.y + b.y) / 2;
	do {
		if (((p.y > py) !== (p.next.y > py)) && p.next.y !== p.y && (px < (p.next.x - p.x) * (py - p.y) / (p.next.y - p.y) + p.x)) inside = !inside;
		p = p.next;
	} while (p !== a);
	return inside;
}
function splitPolygon(a, b) {
	const a2 = new EarNode(a.i, a.x, a.y), b2 = new EarNode(b.i, b.x, b.y), an = a.next, bp = b.prev;
	a.next = b; b.prev = a; a2.next = an; an.prev = a2; b2.next = a2; a2.prev = b2; bp.next = b2; b2.prev = bp;
	return b2;
}
function insertNode(i, x, y, last) {
	const p = new EarNode(i, x, y);
	if (!last) { p.prev = p; p.next = p; }
	else { p.next = last.next; p.prev = last; last.next.prev = p; last.next = p; }
	return p;
}
function removeNode(p) {
	p.next.prev = p.prev; p.prev.next = p.next;
	if (p.prevZ) p.prevZ.nextZ = p.nextZ;
	if (p.nextZ) p.nextZ.prevZ = p.prevZ;
}
function EarNode(i, x, y) { this.i = i; this.x = x; this.y = y; this.prev = null; this.next = null; this.z = null; this.prevZ = null; this.nextZ = null; this.steiner = false; }
function signedArea(data, start, end, dim) {
	let sum = 0;
	for (let i = start, j = end - dim; i < end; i += dim) { sum += (data[j] - data[i]) * (data[i + 1] + data[j + 1]); j = i; }
	return sum;
}

// ---------------------------------------------------------------- ShapeUtils
const ShapeUtils = {
	area(contour) {
		const n = contour.length;
		let a = 0.0;
		for (let p = n - 1, q = 0; q < n; p = q++) a += contour[p].x * contour[q].y - contour[q].x * contour[p].y;
		return a * 0.5;
	},
	isClockWise(pts) { return ShapeUtils.area(pts) < 0; },
	triangulateShape(contour, holes) {
		const vertices = [], holeIndices = [], faces = [];
		removeDupEndPts(contour);
		addContour(vertices, contour);
		let holeIndex = contour.length;
		holes.forEach(removeDupEndPts);
		for (let i = 0; i < holes.length; i++) { holeIndices.push(holeIndex); holeIndex += holes[i].length; addContour(vertices, holes[i]); }
		const triangles = Earcut.triangulate(vertices, holeIndices);
		for (let i = 0; i < triangles.length; i += 3) faces.push(triangles.slice(i, i + 3));
		return faces;
	}
};
function removeDupEndPts(points) { const l = points.length; if (l > 2 && points[l - 1].equals(points[0])) points.pop(); }
function addContour(vertices, contour) { for (let i = 0; i < contour.length; i++) { vertices.push(contour[i].x); vertices.push(contour[i].y); } }

// ---------------------------------------------------------------- ExtrudeGeometry (straight extrusion; no bevels, no path)
const WorldUVGenerator = {
	generateTopUV(geometry, vertices, indexA, indexB, indexC) {
		return [
			new Vector2(vertices[indexA * 3], vertices[indexA * 3 + 1]),
			new Vector2(vertices[indexB * 3], vertices[indexB * 3 + 1]),
			new Vector2(vertices[indexC * 3], vertices[indexC * 3 + 1]),
		];
	},
	generateSideWallUV(geometry, vertices, indexA, indexB, indexC, indexD) {
		const a_x = vertices[indexA * 3], a_y = vertices[indexA * 3 + 1], a_z = vertices[indexA * 3 + 2];
		const b_x = vertices[indexB * 3], b_y = vertices[indexB * 3 + 1], b_z = vertices[indexB * 3 + 2];
		const c_x = vertices[indexC * 3], c_y = vertices[indexC * 3 + 1], c_z = vertices[indexC * 3 + 2];
		const d_x = vertices[indexD * 3], d_y = vertices[indexD * 3 + 1], d_z = vertices[indexD * 3 + 2];
		if (Math.abs(a_y - b_y) < 0.01) {
			return [new Vector2(a_x, 1 - a_z), new Vector2(b_x, 1 - b_z), new Vector2(c_x, 1 - c_z), new Vector2(d_x, 1 - d_z)];
		}
		return [new Vector2(a_y, 1 - a_z), new Vector2(b_y, 1 - b_z), new Vector2(c_y, 1 - c_z), new Vector2(d_y, 1 - d_z)];
	}
};
class ExtrudeGeometry extends BufferGeometry {
	constructor(shapes, options) {
		super();
		this.type = 'ExtrudeGeometry';
		this.parameters = { shapes, options };
		shapes = Array.isArray(shapes) ? shapes : [shapes];
		options = options || {};
		const scope = this, verticesArray = [], uvArray = [];
		for (let i = 0, l = shapes.length; i < l; i++) addShape(shapes[i]);
		this.setAttribute('position', new Float32BufferAttribute(verticesArray, 3));
		this.setAttribute('uv', new Float32BufferAttribute(uvArray, 2));
		this.computeVertexNormals();
		function addShape(shape) {
			const placeholder = [];
			const curveSegments = options.curveSegments !== undefined ? options.curveSegments : 12;
			const steps = options.steps !== undefined ? options.steps : 1;
			let depth = options.depth !== undefined ? options.depth : 100;
			const bevelEnabled = options.bevelEnabled !== undefined ? options.bevelEnabled : true;
			if (options.amount !== undefined) depth = options.amount;
			if (bevelEnabled || options.extrudePath) throw new Error('three_subset: ExtrudeGeometry supports straight extrusion without bevels only');
			const bevelSegments = 0;
			const uvgen = options.UVGenerator !== undefined ? options.UVGenerator : WorldUVGenerator;
			const shapePoints = shape.extractPoints(curveSegments);
			let vertices = shapePoints.shape;
			const holes = shapePoints.holes;
			const reverse = !ShapeUtils.isClockWise(vertices);
			if (reverse) {
				vertices = vertices.reverse();
				for (let h = 0, hl = holes.length; h < hl; h++) { const ahole = holes[h]; if (ShapeUtils.isClockWise(ahole)) holes[h] = ahole.reverse(); }
			}
			const faces = ShapeUtils.triangulateShape(vertices, holes);
			const contour = vertices;
			for (let h = 0, hl = holes.length; h < hl; h++) vertices = vertices.concat(holes[h]);
			const vlen = vertices.length, flen = faces.length;
			for (let i = 0; i < vlen; i++) v(vertices[i].x, vertices[i].y, 0);
			for (let s = 1; s <= steps; s++) for (let i = 0; i < vlen; i++) v(vertices[i].x, vertices[i].y, depth / steps * s);
			buildLidFaces();
			buildSideFaces();
			function buildLidFaces() {
				const start = verticesArray.length / 3;
				for (let i = 0; i < flen; i++) { const face = faces[i]; f3(face[2], face[1], face[0]); }
				for (let i = 0; i < flen; i++) { const face = faces[i]; f3(face[0] + vlen * steps, face[1] + vlen * steps, face[2] + vlen * steps); }
				scope.addGroup(start, verticesArray.length / 3 - start, 0);
			}
			function buildSideFaces() {
				const start = verticesArray.length / 3;
				let layeroffset = 0;
				sidewalls(contour, layeroffset);
				layeroffset += contour.length;
				for (let h = 0, hl = holes.length; h < hl; h++) { const ahole = holes[h]; sidewalls(ahole, layeroffset); layeroffset += ahole.length; }
				scope.addGroup(start, verticesArray.length / 3 - start, 1);
			}
			function sidewalls(contour, layeroffset) {
				let i = contour.length;
				while (--i >= 0) {
					const j = i;
					let k = i - 1;
					if (k < 0) k = contour.length - 1;
					for (let s = 0, sl = (steps + bevelSegments * 2); s < sl; s++) {
						const slen1 = vlen * s, slen2 = vlen * (s + 1);
						const a = layeroffset + j + slen1, b = layeroffset + k + slen1, c = layeroffset + k + slen2, d = layeroffset + j + slen2;
						f4(a, b, c, d);
					}
				}
			}
			function v(x, y, z) { placeholder.push(x); placeholder.push(y); placeholder.push(z); }
			function f3(a, b, c) {
				addVertex(a); addVertex(b); addVertex(c);
				const nextIndex = verticesArray.length / 3;
				const uvs = uvgen.generateTopUV(scope, verticesArray, nextIndex - 3, nextIndex - 2, nextIndex - 1);
				addUV(uvs[0]); addUV(uvs[1]); addUV(uvs[2]);
			}
			function f4(a, b, c, d) {
				addVertex(a); addVertex(b); addVertex(d); addVertex(b); addVertex(c); addVertex(d);
				const nextIndex = verticesArray.length / 3;
				const uvs = uvgen.generateSideWallUV(scope, verticesArray, nextIndex - 6, nextIndex - 3, nextIndex - 2, nextIndex - 1);
				addUV(uvs[0]); addUV(uvs[1]); addUV(uvs[3]); addUV(uvs[1]); addUV(uvs[2]); addUV(uvs[3]);
			}
			function addVertex(index) { verticesArray.push(placeholder[index * 3]); verticesArray.push(placeholder[index * 3 + 1]); verticesArray.push(placeholder[index * 3 + 2]); }
			function addUV(vector2) { uvArray.push(vector2.x); uvArray.push(vector2.y); }
		}
	}
}

// ---------------------------------------------------------------- textures and materials
let _textureId = 0;
class Texture {
	constructor(image, mapping = 300, wrapS = 1001, wrapT = 1001, magFilter = 1006, minFilter = 1008, format = 1023, type = 1009, anisotropy = 1, encoding = 3000) {
		Object.defineProperty(this, 'id', { value: _textureId++ });
		this.uuid = generateUUID(); this.name = ''; this.image = image; this.mipmaps = []; this.mapping = mapping;
		this.wrapS = wrapS; this.wrapT = wrapT; this.magFilter = magFilter; this.minFilter = minFilter; this.anisotropy = anisotropy;
		this.format = format; this.type = type; this.offset = new Vector2(0, 0); this.repeat = new Vector2(1, 1); this.center = new Vector2(0, 0); this.rotation = 0;
		this.generateMipmaps = true; this.premultiplyAlpha = false; this.flipY = true; this.unpackAlignment = 4; this.encoding = encoding; this.version = 0;
	}
	set needsUpdate(value) { if (value === true) this.version++; }
	clone() { const t = new this.constructor(); Object.assign(t, this); t.uuid = generateUUID(); t.offset = this.offset.clone(); t.repeat = this.repeat.clone(); t.center = this.center.clone(); return t; }
	dispose() {}
}
Texture.prototype.isTexture = true;
class CanvasTexture extends Texture {
	constructor(canvas, mapping, wrapS, wrapT, magFilter, minFilter, format, type, anisotropy) { super(canvas, mapping, wrapS, wrapT, magFilter, minFilter, format, type, anisotropy); this.needsUpdate = true; }
}
CanvasTexture.prototype.isCanvasTexture = true;

let _materialId = 0;
const _noCopy = new Set(['id', 'uuid', 'type', 'onBeforeCompile', 'customProgramCacheKey', 'version']);
class Material {
	constructor() {
		Object.defineProperty(this, 'id', { value: _materialId++ });
		this.uuid = generateUUID(); this.name = ''; this.type = 'Material';
		this.fog = true; this.blending = 1; this.side = 0; this.vertexColors = false; this.opacity = 1; this.transparent = false;
		this.depthTest = true; this.depthWrite = true; this.colorWrite = true; this.precision = null;
		this.polygonOffset = false; this.polygonOffsetFactor = 0; this.polygonOffsetUnits = 0;
		this.dithering = false; this.alphaTest = 0; this.alphaToCoverage = false; this.premultipliedAlpha = false;
		this.visible = true; this.toneMapped = true; this.userData = {}; this.version = 0;
	}
	onBuild() {}
	onBeforeCompile() {}
	customProgramCacheKey() { return this.onBeforeCompile.toString(); }
	setValues(values) {
		if (values === undefined) return;
		for (const key in values) {
			const newValue = values[key];
			if (newValue === undefined) { console.warn('THREE.Material: \'' + key + '\' parameter is undefined.'); continue; }
			const currentValue = this[key];
			if (currentValue === undefined) { console.warn('THREE.' + this.type + ': \'' + key + '\' is not a property of this material.'); continue; }
			if (currentValue && currentValue.isColor) currentValue.set(newValue);
			else if ((currentValue && currentValue.isVector3) && (newValue && newValue.isVector3)) currentValue.copy(newValue);
			else this[key] = newValue;
		}
	}
	clone() { return new this.constructor().copy(this); }
	copy(source) {
		// r128 copies its known properties; the shader hooks (onBeforeCompile, customProgramCacheKey) are not copied
		for (const key of Object.keys(source)) {
			if (_noCopy.has(key)) continue;
			const value = source[key];
			if (value && value.isColor) { if (this[key] && this[key].isColor) this[key].copy(value); else this[key] = value.clone(); }
			else if (value && (value.isVector2 || value.isVector3)) this[key] = value.clone();
			else if (key === 'userData') this[key] = JSON.parse(JSON.stringify(value));
			else this[key] = value;
		}
		return this;
	}
	set needsUpdate(value) { if (value === true) this.version++; }
	dispose() {}
}
Material.prototype.isMaterial = true;
class MeshStandardMaterial extends Material {
	constructor(parameters) {
		super();
		this.defines = { 'STANDARD': '' }; this.type = 'MeshStandardMaterial';
		this.color = new Color(0xffffff); this.roughness = 1.0; this.metalness = 0.0; this.map = null; this.lightMap = null; this.lightMapIntensity = 1.0;
		this.aoMap = null; this.aoMapIntensity = 1.0; this.emissive = new Color(0x000000); this.emissiveIntensity = 1.0; this.emissiveMap = null;
		this.bumpMap = null; this.bumpScale = 1; this.normalMap = null; this.normalMapType = 0; this.normalScale = new Vector2(1, 1);
		this.displacementMap = null; this.displacementScale = 1; this.displacementBias = 0; this.roughnessMap = null; this.metalnessMap = null;
		this.alphaMap = null; this.envMap = null; this.envMapIntensity = 1.0; this.refractionRatio = 0.98;
		this.wireframe = false; this.wireframeLinewidth = 1; this.skinning = false; this.morphTargets = false; this.morphNormals = false;
		this.flatShading = false; this.vertexTangents = false;
		this.setValues(parameters);
	}
}
MeshStandardMaterial.prototype.isMeshStandardMaterial = true;
class MeshBasicMaterial extends Material {
	constructor(parameters) {
		super();
		this.type = 'MeshBasicMaterial';
		this.color = new Color(0xffffff); this.map = null; this.lightMap = null; this.lightMapIntensity = 1.0; this.aoMap = null; this.aoMapIntensity = 1.0;
		this.specularMap = null; this.alphaMap = null; this.envMap = null; this.combine = 0; this.reflectivity = 1; this.refractionRatio = 0.98;
		this.wireframe = false; this.wireframeLinewidth = 1; this.skinning = false; this.morphTargets = false;
		this.setValues(parameters);
	}
}
MeshBasicMaterial.prototype.isMeshBasicMaterial = true;
class SpriteMaterial extends Material {
	constructor(parameters) {
		super();
		this.type = 'SpriteMaterial';
		this.color = new Color(0xffffff); this.map = null; this.alphaMap = null; this.rotation = 0; this.sizeAttenuation = true; this.transparent = true;
		this.setValues(parameters);
	}
}
SpriteMaterial.prototype.isSpriteMaterial = true;

Object.assign(THREE, {
	Vector2, Vector3, Quaternion, Euler, Matrix3, Matrix4, Color,
	Object3D, Group, Scene, Mesh, Sprite,
	BufferAttribute, Float32BufferAttribute, Uint16BufferAttribute, Uint32BufferAttribute, InterleavedBuffer, InterleavedBufferAttribute,
	BufferGeometry, BoxGeometry, CylinderGeometry, SphereGeometry, TorusGeometry, LatheGeometry, PlaneGeometry, CircleGeometry, RingGeometry, ExtrudeGeometry,
	BoxBufferGeometry: BoxGeometry, CylinderBufferGeometry: CylinderGeometry, SphereBufferGeometry: SphereGeometry, TorusBufferGeometry: TorusGeometry,
	LatheBufferGeometry: LatheGeometry, PlaneBufferGeometry: PlaneGeometry, CircleBufferGeometry: CircleGeometry, RingBufferGeometry: RingGeometry, ExtrudeBufferGeometry: ExtrudeGeometry,
	Curve, CurvePath, EllipseCurve, ArcCurve, LineCurve, QuadraticBezierCurve, CubicBezierCurve, Path, Shape, ShapeUtils, Earcut,
	Texture, CanvasTexture, Material, MeshStandardMaterial, MeshBasicMaterial, SpriteMaterial,
});
global.THREE = THREE;
})(typeof window !== 'undefined' ? window : globalThis);
