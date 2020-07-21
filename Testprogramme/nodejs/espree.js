espree = require("espree");

source_code = "var i = 42;";

ast = espree.parse(source_code, {});

console.log(ast);

