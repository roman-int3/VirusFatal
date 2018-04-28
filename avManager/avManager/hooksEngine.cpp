#include "stdafx.h"
#include "externs.h"
#include "zdisasm.h"

#define SIZEOFJUMP	5
#define ASMNOP		0x90
#define ASMJMP		0xE9



inline void InsertByte(ULONG Addr, unsigned char Byte)
{
	*((unsigned char*)Addr) = Byte;
}

inline void InsertDword(ULONG Addr, ULONG dWord)
{
	*((PULONG)Addr) = dWord;
}


void GenJmp(ULONG To, ULONG From)
{	
	InsertDword(From + 1, To - From - 5);	// dst - src - 5
	InsertByte(From, ASMJMP);				// jmp	...
}


PVOID Hook(PVOID Addr, PVOID NewFunc)
{
	
	PVOID CallGate, Inst = Addr;
	ULONG cSize = 0, CollectedSpace = 0, CallGateSize = 0; 
	
	if (Addr == NULL)
		return 0;

	while (CollectedSpace < SIZEOFJUMP)
	{
		GetInstLenght(Inst, &cSize);
		_asm push eax
		_asm mov eax,Inst;
		_asm add eax,cSize;
		_asm mov Inst,eax;
		_asm pop eax
		
		CollectedSpace += cSize;
	}

	CallGateSize = CollectedSpace + SIZEOFJUMP;
	CallGate = (PVOID)VirtualAllocEx(GetCurrentProcess(),0, CallGateSize,MEM_COMMIT|MEM_RESERVE,PAGE_EXECUTE_READWRITE);
	DWORD oldProtect;
	VirtualProtect(Addr,CallGateSize,PAGE_EXECUTE_READWRITE,&oldProtect);

	RtlFillMemory(CallGate, ASMNOP, CallGateSize);

	RtlCopyMemory(CallGate, Addr, CollectedSpace);

	RtlFillMemory(Addr, ASMNOP, CollectedSpace);

	GenJmp((ULONG)Addr + SIZEOFJUMP, (ULONG)CallGate + CollectedSpace);
	
	GenJmp((ULONG)NewFunc, (ULONG)Addr);

	return CallGate;
} 


void UnHook(PVOID Addr, PVOID CallGate)
{
	if(Addr && CallGate)
	{

		int gSize ;
		char *pCallGate = (char*)CallGate;
		for(gSize=0;pCallGate[gSize] != 0; gSize++);
		gSize -= SIZEOFJUMP;
		RtlMoveMemory(Addr,CallGate, gSize);
		VirtualFreeEx(GetCurrentProcess(),CallGate,0,MEM_RELEASE);
	}
}
