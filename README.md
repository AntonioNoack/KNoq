# KNoq - Bruteforcing Proofs

Inspired by the Noq (not Coq) project by [Tsoding](https://twitch.tv/tsoding), I created my own "proof assistant", but I focused on automatically finding proofs.
To understand the concept, watch his [Introduction](https://www.youtube.com/watch?v=Ra_Fk7JFMoo), and see [His GitHub project](https://github.com/tsoding/Noq).

My project uses [my game engine](https://github.com/AntonioNoack/RemsEngine), because it's my toolbox for me :).
If you need this project without the bloat ^^, just extract the functions from my engine.

## How it works

All rules are applied to every term at every viable place. This creates new terms, which are then doing the same procedure.
Terms with shorter length, and more direct ancestry from the input, are searched earlier. In my project, I call this length/ancestry penalty "cost".
When the output has been reached, a proof has been found.

## Rules for the following examples
```
swap(pair(a,b))=pair(b,a)
2=1+1
3=2+1
4=3+1
a+b=b+a
a+0=0+a
a*b=b*a
a+a=2*a
s(a)=a+1
(a+b)+c=a+(b+c)
a^2=a*a
a*(b+c)=a*b+a*c
```

## Example: (a+b)² = a²+2ab+b²

```
checking cost 20, 4 checked, (b+a)^2
found path after 601 entries, cost 32
--- (a+b)^2 = a^2+2*a*b+b^2 ---
(0) (a+b)^2
(1) (a+b)*(a+b)
(2) (a+b)*a+(a+b)*b
(3) a*(a+b)+(a+b)*b
(4) (a*a+a*b)+(a+b)*b
(5) (a^2+a*b)+(a+b)*b
(6) (a^2+b*a)+b*(a+b)
(7) (a^2+b*a)+(b*a+b*b)
(8) (a^2+b*a)+(b*a+b^2)
(9) ((a^2+b*a)+b*a)+b^2
(10) (a^2+(b*a+b*a))+b^2
(11) (a^2+b*(a+a))+b^2
(12) (a^2+(a+a)*b)+b^2
(13) (a^2+(2*a)*b)+b^2
```

## Example: 4=4*1 without a=(a+1)
```
checking cost 20, 48 checked, s(1+1)+1
found path after 438 entries, cost 18
--- 4 = 4*1 ---
(0) 4
(1) 3+1
(2) (2+1)+1
(3) 2+(1+1)
(4) 2+2*1
(5) (1+1)+(1+1)*1
(6) 2*1+(2*1)*1
(7) 1*2+1*(2*1)
(8) 1*(2+2*1)
(9) 1*(2+(1+1))
(10) 1*((2+1)+1)
(11) 1*(3+1)
(12) 1*4
(13) 4*1
```

## Example: 3=1+1
```
checking cost 20, 22 checked, (1+1)*1+1
checking cost 40, 742 checked, (((1+1)*1)*1)*1+1
checking cost 58, 18902 checked, 1^(1*(2*1))+(1+1^s(1))
checking cost 64, 63025 checked, 1+(((1^s(1)*1)*1)*2)*1
exceeded trial limit of 100000, last cost: 67
--- 3 =?= 1+1 ---
```