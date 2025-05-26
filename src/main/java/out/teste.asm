.686
.model flat, stdcall
option casemap :none

include \masm32\include\windows.inc
include \masm32\include\kernel32.inc
include \masm32\include\masm32.inc
include \masm32\include\msvcrt.inc
includelib \masm32\lib\kernel32.lib
includelib \masm32\lib\masm32.lib
includelib \masm32\lib\msvcrt.lib
include \masm32\macros\macros.asm

.data
n dd 0
nome db 256 dup(0)
naoTerminou db 0
MAXITER equ 10
hexa dw 10h
.code
start:
invoke crt_printf, addr str1
invoke ExitProcess, 0
end start
