function inc(x) {
  return x + 1
}

var a = 10
var b = a;
// Let's call inc() with normal semantics
while (a == b && a < 15) {
  a = inc(a);
  b = b + 1;
}
c = a;
// Run inc() and alter it's return type using the instrument
print("abcdefg" + c)
