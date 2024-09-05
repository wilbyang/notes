## steps

1. `go get -u github.com/dvyukov/go-fuzz/go-fuzz`
2. `go get github.com/dvyukov/go-fuzz/go-fuzz-build`
3. `go get -d github.com/dvyukov/go-fuzz-corpus`
4. `cd ~/go/src/github.com/dvyukov/go-fuzz-corpus/png`
5. `go-fuzz-build` or `go-fuzz-build -libfuzzer`
6. `go-fuzz`

## p.s.

1. Go草案：在go工具链中增加模糊测试支持 - https://github.com/golang/proposal/blob/master/design/40307-fuzzing.md

```
func FuzzXXX(f *testing.F) {
	... ...
}
```

2. Go原生支持fuzz test的提案被accept - https://github.com/golang/go/issues/44551


## refs

1. https://adalogics.com/blog/getting-started-with-go-fuzz

