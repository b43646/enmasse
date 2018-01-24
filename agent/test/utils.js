/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

var assert = require('assert');
var myutils = require('../lib/utils.js');

describe('replace', function() {
    it('updates matching string', function(done) {
        var list = ['a', 'b', 'c'];
        assert.equal(myutils.replace(list, 'x', function (o) { return o === 'b'}), true);
        assert.deepEqual(list, ['a', 'x', 'c']);
        done();
    });
    it('returns false when there is no match', function(done) {
        var list = ['a', 'b', 'c'];
        assert.equal(myutils.replace(list, 'x', function (o) { return o === 'foo'}), false);
        assert.deepEqual(list, ['a', 'b', 'c']);
        done();
    });
});

describe('merge', function() {
    it('combines fields for multiple objects', function(done) {
        var obj = myutils.merge({'foo':10}, {'bar':9});
        assert.deepEqual(obj, {'foo':10, 'bar':9});
        done();
    });
});

describe('kubernetes_name', function () {
    it ('does not affect names without special chars and under 64 chars of length', function (done) {
        var valid = ['foo', 'foo-bar', 'abcdefghiklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-x'];
        for (var i in valid) {
            assert.equal(valid[i], myutils.kubernetes_name(valid[i]));
        }
        done();
    });
    it ('removes invalid chars', function (done) {
        var invalid = '!"£$%^&*()_?><~#@\':;`/\|';
        var input = 'a!b"c£d$e%f^g&h*i()j_?><k~#l@\'m:n;opq`/r\s|t';
        var output = myutils.kubernetes_name(input);
        assert.notEqual(output, input);
        for (var i = 0; i < invalid.length; i++) {
            assert(output.indexOf(invalid.charAt(i)) < 0, 'invalid char ' + invalid.charAt(i) + ' found in ' + output);
        }
        done();
    });
    it ('differentiates modified names', function (done) {
        var a = myutils.kubernetes_name('foo@bar');
        var b = myutils.kubernetes_name('foo~bar');
        var c = myutils.kubernetes_name('foo/bar/baz');
        assert.notEqual(a, b);
        assert.notEqual(a, c);
        assert.notEqual(b, c);
        done();
    });
    it ('gives same output for same input', function (done) {
        var a = myutils.kubernetes_name('foo@bar');
        var b = myutils.kubernetes_name('foo~bar');
        var c = myutils.kubernetes_name('foo/bar/baz');
        assert.equal(a, myutils.kubernetes_name('foo@bar'));
        assert.equal(b, myutils.kubernetes_name('foo~bar'));
        assert.equal(c, myutils.kubernetes_name('foo/bar/baz'));
        done();
    });
    it ('truncates names without special chars over 64 chars of length', function (done) {
        var too_long = 'abcdefghiklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-xxxxxxx';
        assert(too_long.length > 64);
        var name = myutils.kubernetes_name(too_long);
        assert(name.length < 64);
        done();
    });
    it ('truncates names with special chars over 64 chars of length', function (done) {
        var too_long = 'a!"£$%^&*()_+=-bcdefghiklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-';
        assert(too_long.length > 64);
        var name = myutils.kubernetes_name(too_long);
        assert(name.length < 64);
        var also_too_long = 'a!"£$%^&*()_+==bcdefghiklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-';
        var another_name = myutils.kubernetes_name(also_too_long);
        assert(another_name.length < 64);
        assert.notEqual(name, another_name);
        done();
    });
});
