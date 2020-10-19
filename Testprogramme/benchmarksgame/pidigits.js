/*
 * The Computer Language Benchmarks Game
 * https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
 *
 * contributed by Denis Gribov
 *    a translation of the C program contributed by Mr Ledhug
 */

(function main() {

    let n = +process.argv[2] || 10000,
        i = 0,
        k = 0,
        acc = 0n,
        den = 1n,
        num = 1n;

    const chr_0 = "0".charCodeAt(),
        chr_t = "\t".charCodeAt(),
        chr_n = "\n".charCodeAt(),
        chr_c = ":".charCodeAt();

    // preallocated buffer size
    let bufsize = (10/*value of pi*/ + 2/*\t:*/ 
        + n.toString().length/*index of slice*/ + 1/*\n*/) * (n / 10)/*line count*/;
    // croped buffer size
    for (let i = 10, ii = 10 ** (Math.log10(n) >>> 0); i < ii; i *= 10) {
        bufsize -= i - 1;
    }

    let buf = Buffer.allocUnsafe(bufsize),
        bufoffs = 0;

    while (i < n) {
        k++;

        //#region nextTerm(k)
        let k2 = BigInt((k << 1) + 1);
        acc += num << 1n;
        acc = k2 * acc;
        den = k2 * den;
        num = BigInt(k) * num;
        //#endregion

        if (num > acc) continue;

        //#region extractDigit(3);
        let tmp = 3n * num + acc;
        let d3 = tmp / den;
        //#endregion

        //#region extractDigit(4);
        tmp = tmp + num;
        let d4 = tmp / den;
        //#endregion

        if (d3 !== d4) continue;

        buf.writeInt8(Number(d3) + chr_0, bufoffs++);

        if (++i % 10 === 0) {
            buf.writeInt8(chr_t, bufoffs++);
            buf.writeInt8(chr_c, bufoffs++);

            let str = i.toString();
            buf.write(str, bufoffs, bufoffs += str.length);

            buf.writeInt8(chr_n, bufoffs++);
        }

        //#region eliminateDigit(d3)
        acc -= d3 * den;
        acc = 10n * acc;
        num = 10n * num;
        //#endregion
    }

    process.stdout.write(buf);
})();
