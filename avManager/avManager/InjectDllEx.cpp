
#include "stdafx.h"
#include "InjectDllEx.h"

VOID CInjectDllEx::ProcessRelocs(PIMAGE_BASE_RELOCATION PRelocs)
{
    PIMAGE_BASE_RELOCATION PReloc = PRelocs;
    DWORD RelocsSize = ImageNtHeaders->OptionalHeader.DataDirectory[IMAGE_DIRECTORY_ENTRY_BASERELOC].Size;
    DWORD ModCount;
    PWORD Reloc;
    DWORD RelocLoop;
    while(DWORD(PReloc) - DWORD(PRelocs) < RelocsSize)
    {
        ModCount = (PReloc->SizeOfBlock - sizeof(*PReloc)) / 2;
        Reloc = PWORD(DWORD(PReloc) + sizeof(*PReloc));
        for(RelocLoop = 0; RelocLoop <= ModCount - 1; RelocLoop++)
        {
            if(*Reloc & 0xf000)
                *PDWORD(DWORD(ImageBase) + PReloc->VirtualAddress + (*Reloc & 0x0fff)) += ImageBaseDelta;
            Reloc++;
        }
        PReloc = PIMAGE_BASE_RELOCATION(Reloc);
    }
}


VOID CInjectDllEx::Add(PCHAR pNewLib)
{
    LinkedList *pElement = new LinkedList;
    lstrcpy(pElement->Lib,pNewLib);
    pElement->pNext = 0;
    
    LinkedList *pCurrent;
    if(!pHead)
    {
        pHead = pElement;
        return;
    }

    pCurrent = pHead;
    while(pCurrent->pNext)
        pCurrent = pCurrent->pNext;
    pCurrent->pNext = pElement;
}

BOOL CInjectDllEx::Find(PCHAR pLib)
{
    LinkedList *pElement = pHead;
    while(pElement)
    {
        if(!lstrcmpi(pElement->Lib,pLib))
            return TRUE;
        pElement = pElement->pNext;
    }
    return FALSE;
}

VOID CInjectDllEx::Clear()
{
    LinkedList *pElement = pHead, *pTemp = 0;
    while(pElement)
    {
        pTemp = pElement->pNext;
        delete pElement;
        pElement = pTemp;
    }
    pHead = 0;
}


BOOL CInjectDllEx::InjectDllFromFile(PCHAR ModulePath)
{
    #pragma pack(1)
    struct
    {
        BYTE PushCommand;
        DWORD PushArgument;
        WORD CallCommand;
        DWORD CallAddr;
        BYTE PushExitThread;
        DWORD ExitThreadArg;
        WORD CallExitThread;
        DWORD CallExitThreadAddr;
        LPVOID AddrLoadLibrary;
        LPVOID AddrExitThread;
        CHAR LibraryName[MAX_PATH + 1];
    }Inject;
    #pragma pack()

    LPVOID Memory = VirtualAllocEx(this->ProcInfo.hProcess,0,sizeof(Inject),MEM_COMMIT,PAGE_EXECUTE_READWRITE);
    if(!Memory)
        return FALSE;
    DWORD Code = DWORD(Memory);

    Inject.PushCommand        = 0x68;
    Inject.PushArgument       = Code + 0x1E;
    Inject.CallCommand        = 0x15FF;
    Inject.CallAddr           = Code + 0x16;
    Inject.PushExitThread     = 0x50;
    Inject.ExitThreadArg      = 0x90909090;
    Inject.CallExitThread     = 0x15FF;
    Inject.CallExitThreadAddr = Code + 0x1A;
    HMODULE hKernel32         = GetModuleHandle("kernel32.dll");
    Inject.AddrLoadLibrary    = GetProcAddress(hKernel32,"LoadLibraryA");
    Inject.AddrExitThread     = GetProcAddress(hKernel32,"ExitThread");
    lstrcpy(Inject.LibraryName,ModulePath);

    WriteProcessMemory(this->ProcInfo.hProcess,Memory,&Inject,sizeof(Inject),0);
    CONTEXT Context;
    Context.ContextFlags = CONTEXT_FULL;
    BOOL bResumed = FALSE;

    if(!bResumed)
    {
        HANDLE hThread = CreateRemoteThread(this->ProcInfo.hProcess,0,0,(LPTHREAD_START_ROUTINE)Memory,0,0,0);
        GetLastError();
		if(!hThread)
		{
			VirtualFreeEx(this->ProcInfo.hProcess,Memory,sizeof(Inject),MEM_RELEASE);
			return FALSE;
		}
		
		WaitForSingleObject(hThread,INFINITE);
		DWORD hLoadMod;
		GetExitCodeThread(hThread,&hLoadMod);
        CloseHandle(hThread);
		
    }

	VirtualFreeEx(this->ProcInfo.hProcess,Memory,sizeof(Inject),MEM_RELEASE);
	ResumeThread(this->ProcInfo.hThread);
    return TRUE;
}

LPVOID CInjectDllEx::InjectMemory(LPVOID Memory,DWORD Size)
{
    LPVOID Result = VirtualAllocEx(this->ProcInfo.hProcess,0,Size,MEM_COMMIT|MEM_RESERVE,PAGE_EXECUTE_READWRITE);
    if(WriteProcessMemory(this->ProcInfo.hProcess,Result,Memory,Size,0))
        return Result;
    return 0;
}

DWORD CInjectDllEx::SizeOfCode(LPVOID Code)
{
    WORD Opcode;
    BYTE Modrm;
    BOOL Fixed, AddressOveride;
    DWORD Last, OperandOveride, Flags, Rm, Size, Extend;

    WORD Opcodes1[256] = {
        0x4211, 0x42E4, 0x2011, 0x20E4, 0x8401, 0x8C42, 0x0000, 0x0000, 0x4211, 0x42E4,
        0x2011, 0x20E4, 0x8401, 0x8C42, 0x0000, 0x0000, 0x4211, 0x42E4, 0x2011, 0x20E4,
        0x8401, 0x8C42, 0x0000, 0x0000, 0x4211, 0x42E4, 0x2011, 0x20E4, 0x8401, 0x8C42,
        0x0000, 0x0000, 0x4211, 0x42E4, 0x2011, 0x20E4, 0x8401, 0x8C42, 0x0000, 0x8000,
        0x4211, 0x42E4, 0x2011, 0x20E4, 0x8401, 0x8C42, 0x0000, 0x8000, 0x4211, 0x42E4,
        0x2011, 0x20E4, 0x8401, 0x8C42, 0x0000, 0x8000, 0x0211, 0x02E4, 0x0011, 0x00E4,
        0x0401, 0x0C42, 0x0000, 0x8000, 0x6045, 0x6045, 0x6045, 0x6045, 0x6045, 0x6045,
        0x6045, 0x6045, 0x6045, 0x6045, 0x6045, 0x6045, 0x6045, 0x6045, 0x6045, 0x6045,
        0x0045, 0x0045, 0x0045, 0x0045, 0x0045, 0x0045, 0x0045, 0x0045, 0x6045, 0x6045,
        0x6045, 0x6045, 0x6045, 0x6045, 0x6045, 0x6045, 0x0000, 0x8000, 0x00E4, 0x421A,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0C00, 0x2CE4, 0x0400, 0x24E4, 0x0000, 0x0000,
        0x0000, 0x0000, 0x1400, 0x1400, 0x1400, 0x1400, 0x1400, 0x1400, 0x1400, 0x1400,
        0x1400, 0x1400, 0x1400, 0x1400, 0x1400, 0x1400, 0x1400, 0x1400, 0x0510, 0x0DA0,
        0x0510, 0x05A0, 0x0211, 0x02E4, 0xA211, 0xA2E4, 0x4211, 0x42E4, 0x2011, 0x20E4,
        0x42E3, 0x20E4, 0x00E3, 0x01A0, 0x0000, 0xE046, 0xE046, 0xE046, 0xE046, 0xE046,
        0xE046, 0xE046, 0x8000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x8000,
        0x8101, 0x8142, 0x0301, 0x0342, 0x0000, 0x0000, 0x0000, 0x0000, 0x0401, 0x0C42,
        0x0000, 0x0000, 0x8000, 0x8000, 0x0000, 0x0000, 0x6404, 0x6404, 0x6404, 0x6404,
        0x6404, 0x6404, 0x6404, 0x6404, 0x6C45, 0x6C45, 0x6C45, 0x6C45, 0x6C45, 0x6C45,
        0x6C45, 0x6C45, 0x4510, 0x45A0, 0x0800, 0x0000, 0x20E4, 0x20E4, 0x4510, 0x4DA0,
        0x0000, 0x0000, 0x0800, 0x0000, 0x0000, 0x0400, 0x0000, 0x0000, 0x4110, 0x41A0,
        0x4110, 0x41A0, 0x8400, 0x8400, 0x0000, 0x8000, 0x0008, 0x0008, 0x0008, 0x0008,
        0x0008, 0x0008, 0x0008, 0x0008, 0x1400, 0x1400, 0x1400, 0x1400, 0x8401, 0x8442,
        0x0601, 0x0642, 0x1C00, 0x1C00, 0x0000, 0x1400, 0x8007, 0x8047, 0x0207, 0x0247,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0008, 0x0008, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x4110, 0x01A0};

    WORD Opcodes2[256] = {
        0x0118, 0x0120, 0x20E4, 0x20E4, 0xFFFF, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0xFFFF, 0xFFFF, 0xFFFF, 0x0110, 0x0000, 0x052D, 0x003F, 0x023F, 0x003F, 0x023F,
        0x003F, 0x003F, 0x003F, 0x023F, 0x0110, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF,
        0xFFFF, 0xFFFF, 0x4023, 0x4023, 0x0223, 0x0223, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF,
        0x003F, 0x023F, 0x002F, 0x023F, 0x003D, 0x003D, 0x003F, 0x003F, 0x0000, 0x8000,
        0x8000, 0x8000, 0x0000, 0x0000, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF,
        0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0x20E4, 0x20E4, 0x20E4, 0x20E4, 0x20E4, 0x20E4,
        0x20E4, 0x20E4, 0x20E4, 0x20E4, 0x20E4, 0x20E4, 0x20E4, 0x20E4, 0x20E4, 0x20E4,
        0x4227, 0x003F, 0x003F, 0x003F, 0x003F, 0x003F, 0x003F, 0x003F, 0x003F, 0x003F,
        0x003F, 0x003F, 0x003F, 0x003F, 0x003F, 0x003F, 0x00ED, 0x00ED, 0x00ED, 0x00ED,
        0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED,
        0x0065, 0x00ED, 0x04ED, 0x04A8, 0x04A8, 0x04A8, 0x00ED, 0x00ED, 0x00ED, 0x0000,
        0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0x0265, 0x02ED, 0x1C00, 0x1C00,
        0x1C00, 0x1C00, 0x1C00, 0x1C00, 0x1C00, 0x1C00, 0x1C00, 0x1C00, 0x1C00, 0x1C00,
        0x1C00, 0x1C00, 0x1C00, 0x1C00, 0x4110, 0x4110, 0x4110, 0x4110, 0x4110, 0x4110,
        0x4110, 0x4110, 0x4110, 0x4110, 0x4110, 0x4110, 0x4110, 0x4110, 0x4110, 0x4110,
        0x0000, 0x0000, 0x8000, 0x02E4, 0x47E4, 0x43E4, 0xC211, 0xC2E4, 0x0000, 0x0000,
        0x0000, 0x42E4, 0x47E4, 0x43E4, 0x0020, 0x20E4, 0xC211, 0xC2E4, 0x20E4, 0x42E4,
        0x20E4, 0x22E4, 0x2154, 0x211C, 0xFFFF, 0xFFFF, 0x05A0, 0x42E4, 0x20E4, 0x20E4,
        0x2154, 0x211C, 0xA211, 0xA2E4, 0x043F, 0x0224, 0x0465, 0x24AC, 0x043F, 0x8128,
        0x6005, 0x6005, 0x6005, 0x6005, 0x6005, 0x6005, 0x6005, 0x6005, 0xFFFF, 0x00ED,
        0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x02ED, 0x20AC, 0x00ED, 0x00ED, 0x00ED, 0x00ED,
        0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED,
        0x003F, 0x02ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED,
        0xFFFF, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED,
        0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x00ED, 0x0000};

    WORD Opcodes3[10][16] = {
        {0x0510, 0xFFFF, 0x4110, 0x4110, 0x8110, 0x8110, 0x8110, 0x8110, 0x0510, 0xFFFF, 0x4110, 0x4110, 0x8110, 0x8110, 0x8110, 0x8110},
        {0x0DA0, 0xFFFF, 0x41A0, 0x41A0, 0x81A0, 0x81A0, 0x81A0, 0x81A0, 0x0DA0, 0xFFFF, 0x41A0, 0x41A0, 0x81A0, 0x81A0, 0x81A0, 0x81A0},
        {0x0120, 0x0120, 0x0120, 0x0120, 0x0120, 0x0120, 0x0120, 0x0120, 0x0036, 0x0036, 0x0030, 0x0030, 0x0036, 0x0036, 0x0036, 0x0036},
        {0x0120, 0xFFFF, 0x0120, 0x0120, 0x0110, 0x0118, 0x0110, 0x0118, 0x0030, 0x0030, 0x0000, 0x0030, 0x0000, 0x0000, 0x0000, 0x0000},
        {0x0120, 0x0120, 0x0120, 0x0120, 0x0120, 0x0120, 0x0120, 0x0120, 0x0036, 0x0036, 0x0036, 0x0036, 0xFFFF, 0x0000, 0xFFFF, 0xFFFF},
        {0x0120, 0xFFFF, 0x0120, 0x0120, 0xFFFF, 0x0130, 0xFFFF, 0x0130, 0x0036, 0x0036, 0x0036, 0x0036, 0x0000, 0x0036, 0x0036, 0x0000},
        {0x0128, 0x0128, 0x0128, 0x0128, 0x0128, 0x0128, 0x0128, 0x0128, 0x0236, 0x0236, 0x0030, 0x0030, 0x0236, 0x0236, 0x0236, 0x0236},
        {0x0128, 0xFFFF, 0x0128, 0x0128, 0x0110, 0xFFFF, 0x0110, 0x0118, 0x0030, 0x0030, 0x0030, 0x0030, 0x0030, 0x0030, 0xFFFF, 0xFFFF},
        {0x0118, 0x0118, 0x0118, 0x0118, 0x0118, 0x0118, 0x0118, 0x0118, 0x0236, 0x0236, 0x0030, 0x0236, 0x0236, 0x0236, 0x0236, 0x0236},
        {0x0118, 0xFFFF, 0x0118, 0x0118, 0x0130, 0x0128, 0x0130, 0x0128, 0x0030, 0x0030, 0x0030, 0x0030, 0x0000, 0x0036, 0x0036, 0xFFFF}};

    Last = DWORD(Code);
    if(Code)
    {
        AddressOveride = FALSE;
        Fixed = FALSE;
        OperandOveride = 4;
        Extend = 0;
        do
        {
            Opcode = *(BYTE*)Code;
            Code = LPVOID(DWORD(Code) + 1);
            if(Opcode == 0x66)
                OperandOveride = 2;
            else if(Opcode == 0x67)
                AddressOveride = TRUE;
            else
                if(!(Opcode & 0xE7 == 0x26))
                    if(Opcode != 0x64 && Opcode != 0x65)
                        Fixed = TRUE;
        }while(!Fixed);
        if(Opcode == 0xF)
        {
            Opcode = *(BYTE*)Code;
            Flags = Opcodes2[Opcode];
            Opcode += 0xF00;
            Code = LPVOID(DWORD(Code) + 1);
        }
        else
            Flags = Opcodes1[Opcode];

        if(Flags & 0x38)
        {
            Modrm = *(BYTE*)Code;
            Rm = Modrm & 7;
            Code = LPVOID(DWORD(Code) + 1);
        
            switch(Modrm & 0xC0)
            {
            case 0x40:
                Size = 1;
                break;
            case 0x80:
                if(AddressOveride)
                    Size = 2;
                else
                    Size = 4;
                break;
            default:
                Size = 0;
            }

            if(!((Modrm & 0xC0) != 0xC0 && AddressOveride))
            {
                if(Rm == 4 && (Modrm & 0xC0) != 0xC0)
                    Rm = *(BYTE*)Code & 7;
                if(Modrm & 0xC0 == 0 && Rm == 5)
                    Size = 4;
                Code = LPVOID(DWORD(Code) + Size);
            }
        
            if(Flags & 0x38 == 8)
            {
                switch(Opcode)
                {
                case 0xf6:
                    Extend = 0;
                    break;
                case 0xf7:
                    Extend = 1;
                    break;
                case 0xd8:
                    Extend = 2;
                    break;
                case 0xd9:
                    Extend = 3;
                    break;
                case 0xda:
                    Extend = 4;
                    break;
                case 0xdb:
                    Extend = 5;
                    break;
                case 0xdc:
                    Extend = 6;
                    break;
                case 0xdd:
                    Extend = 7;
                    break;
                case 0xde:
                    Extend = 8;
                    break;
                case 0xdf:
                    Extend = 9;
                }
                if(Modrm & 0xC0 != 0xC0)
                    Flags = Opcodes3[Extend][(Modrm >> 3) & 7];
                else
                    Flags = Opcodes3[Extend][((Modrm >> 3) & 7) + 8];
            }
        }

        switch(Flags & 0xC00)
        {
        case 0x0400:
            Code = LPVOID(DWORD(Code) + 1);
            break;
        case 0x0800:
            Code = LPVOID(DWORD(Code) + 2);
            break;
        case 0x0C00:
            Code = LPVOID(DWORD(Code) + OperandOveride);
            break;
        default:
            {
                switch(Opcode)
                {
                case 0x9a:
                case 0xea:
                    Code = LPVOID(DWORD(Code) + OperandOveride + 2);
                    break;
                case 0xc8:
                    Code = LPVOID(DWORD(Code) + 3);
                    break;
                case 0xa0:
                case 0xa1:
                case 0xa2:
                case 0xa3:
                    {
                        if(AddressOveride)
                            Code = LPVOID(DWORD(Code) + 2);
                        else
                            Code = LPVOID(DWORD(Code) + 4);
                    }
                }
            }
        }
    }
    return DWORD(Code) - Last;
}

DWORD CInjectDllEx::SizeOfProc(LPVOID Proc)
{
    DWORD Length;
    DWORD Result = 0;
    do
    {
        Length = SizeOfCode(Proc);
        Result += Length;
        if(Length == 1 && *(BYTE*)Proc == 0xC3)
            break;
        Proc = LPVOID(DWORD(Proc) + Length);
    }while(Length);
    return Result;
}

HANDLE CInjectDllEx::InjectThread(LPVOID lpThread,LPVOID Info,DWORD InfoLen)
{
    LPVOID pInfo = InjectMemory(Info,InfoLen);
    LPVOID pThread = InjectMemory(lpThread,SizeOfProc(lpThread));
    return CreateRemoteThread(this->ProcInfo.hProcess,0,0,(LPTHREAD_START_ROUTINE)pThread,pInfo,0,0);
}

PCHAR CInjectDllEx::InjectString(PCHAR Text)
{
    PCHAR Result = (PCHAR)VirtualAllocEx(this->ProcInfo.hProcess,0,lstrlen(Text) + 1,MEM_COMMIT|MEM_RESERVE,PAGE_EXECUTE_READWRITE);
    if(WriteProcessMemory(this->ProcInfo.hProcess,Result,Text,lstrlen(Text) + 1,0))
        return Result;
    return 0;
}

VOID WINAPI CInjectDllEx::GetProcAddrExThread(LPVOID lpParameter)
{
    TGetProcAddrExInfo GetProcAddrExInfo = *(TGetProcAddrExInfo*)lpParameter;
    __asm
    {
        push GetProcAddrExInfo.lpModuleName
        call GetProcAddrExInfo.pGetModuleHandle
        push GetProcAddrExInfo.lpProcName
        push eax
        call GetProcAddrExInfo.pGetProcAddress
        push eax
        call GetProcAddrExInfo.pExitThread
    }
}

LPVOID CInjectDllEx::GetProcAddressEx(PCHAR lpModuleName,PCHAR lpProcName,DWORD dwProcLen)
{
    TGetProcAddrExInfo GetProcAddrExInfo;
    HMODULE hKernel32 = GetModuleHandle("kernel32.dll");

    GetProcAddrExInfo.pGetModuleHandle = GetProcAddress(hKernel32,"GetModuleHandleA");
    GetProcAddrExInfo.pGetProcAddress  = GetProcAddress(hKernel32,"GetProcAddress");
    GetProcAddrExInfo.pExitThread      = GetProcAddress(hKernel32,"ExitThread");

    if(dwProcLen == 4)
        GetProcAddrExInfo.lpProcName = lpProcName;
    else
        GetProcAddrExInfo.lpProcName = InjectMemory(lpProcName,dwProcLen);
    
    GetProcAddrExInfo.lpModuleName = InjectString(lpModuleName);

    HANDLE hThread = InjectThread(GetProcAddrExThread,&GetProcAddrExInfo,sizeof(GetProcAddrExInfo));
    if(hThread)
    {
        WaitForSingleObject(hThread,INFINITE);
        DWORD ExitCode;
        GetExitCodeThread(hThread,&ExitCode);
        return LPVOID(ExitCode);
    }
    return 0;
}

BOOL CInjectDllEx::IsImportByOrdinal(DWORD ImportDescriptor)
{
    return ImportDescriptor & IMAGE_ORDINAL_FLAG32;
}

VOID CInjectDllEx::ProcessImports(PIMAGE_IMPORT_DESCRIPTOR PImports)
{
    PIMAGE_IMPORT_DESCRIPTOR PImport = PImports;
    PCHAR PLibName;
    PDWORD Import;
    PCHAR PImportedName;
    LPVOID ProcAddress;
    while(PImport->Name)
    {
        PLibName = PCHAR(DWORD(PImport->Name) + DWORD(ImageBase));
        if(!Find(PLibName))
        {
            InjectDllFromFile(PLibName);
            Add(PLibName);
        }
        if(!PImport->TimeDateStamp)
            Import = PDWORD(PImport->FirstThunk + DWORD(ImageBase));
        else
            Import = PDWORD(PImport->OriginalFirstThunk + DWORD(ImageBase));

        while(*Import)
        {
            if(IsImportByOrdinal(*Import))
                ProcAddress = GetProcAddressEx(PLibName,PCHAR(*Import & 0xffff),4);
            else
            {
                PImportedName = PCHAR(*Import + DWORD(ImageBase) + IMPORTED_NAME_OFFSET);
                ProcAddress = GetProcAddressEx(PLibName,PImportedName,lstrlen(PImportedName));
            }
            *(LPVOID*)Import = ProcAddress;
            Import++;
        }
        PImport++;
    }
    Clear();
}

DWORD CInjectDllEx::GetSectionProtection(DWORD ImageScn)
{
    DWORD Result = 0;
    if(ImageScn & IMAGE_SCN_MEM_NOT_CACHED)
        Result |= PAGE_NOCACHE;
    if(ImageScn & IMAGE_SCN_MEM_EXECUTE)
    {
        if(ImageScn & IMAGE_SCN_MEM_READ)
        {
            if(ImageScn & IMAGE_SCN_MEM_WRITE)
                Result |= PAGE_EXECUTE_READWRITE;
            else
                Result |= PAGE_EXECUTE_READ;
        }
        else if(ImageScn & IMAGE_SCN_MEM_WRITE)
            Result |= PAGE_EXECUTE_WRITECOPY;
        else
            Result |= PAGE_EXECUTE;
    }
    else if(ImageScn & IMAGE_SCN_MEM_READ)
    {
        if(ImageScn & IMAGE_SCN_MEM_WRITE)
            Result |= PAGE_READWRITE;
        else
            Result |= PAGE_READONLY;
    }
    else if(ImageScn & IMAGE_SCN_MEM_WRITE)
        Result |= PAGE_WRITECOPY;
    else
        Result |= PAGE_NOACCESS;
    return Result;
}

VOID CInjectDllEx::MapLibrary(LPVOID Dest,LPVOID Src)
{
    ImageNtHeaders = PIMAGE_NT_HEADERS(DWORD(Src) + DWORD(PIMAGE_DOS_HEADER(Src)->e_lfanew));
    ImageBase = VirtualAlloc(Dest,ImageNtHeaders->OptionalHeader.SizeOfImage,MEM_RESERVE,PAGE_NOACCESS);
    ImageBaseDelta = DWORD(ImageBase) - ImageNtHeaders->OptionalHeader.ImageBase;
    LPVOID SectionBase = VirtualAlloc(ImageBase,ImageNtHeaders->OptionalHeader.SizeOfHeaders,MEM_COMMIT,PAGE_READWRITE);
    memmove(SectionBase,Src,ImageNtHeaders->OptionalHeader.SizeOfHeaders);
    VirtualProtect(SectionBase,ImageNtHeaders->OptionalHeader.SizeOfHeaders,PAGE_READONLY,0);
    PIMAGE_SECTION_HEADER PSections = PIMAGE_SECTION_HEADER(PCHAR(&(ImageNtHeaders->OptionalHeader)) + ImageNtHeaders->FileHeader.SizeOfOptionalHeader);

    DWORD VirtualSectionSize, RawSectionSize;
    for(INT SectionLoop = 0; SectionLoop <= ImageNtHeaders->FileHeader.NumberOfSections - 1; SectionLoop++)
    {
        VirtualSectionSize = PSections[SectionLoop].Misc.VirtualSize;
        RawSectionSize = PSections[SectionLoop].SizeOfRawData;
        if(VirtualSectionSize < RawSectionSize)
        {
            VirtualSectionSize = VirtualSectionSize ^ RawSectionSize;
            RawSectionSize = VirtualSectionSize ^ RawSectionSize;
            VirtualSectionSize = VirtualSectionSize ^ RawSectionSize;
        }
        SectionBase = VirtualAlloc(PSections[SectionLoop].VirtualAddress + PCHAR(ImageBase),VirtualSectionSize,MEM_COMMIT,PAGE_READWRITE);
        ZeroMemory(SectionBase,VirtualSectionSize);
        memmove(SectionBase,PCHAR(Src) + PSections[SectionLoop].PointerToRawData,RawSectionSize);
    }
    _DllProcAddress = LPVOID(ImageNtHeaders->OptionalHeader.AddressOfEntryPoint + DWORD(ImageBase));
    _DllProc = TDllEntryProc(_DllProcAddress);  
    _ImageBase = ImageBase;
    _ImageSize = ImageNtHeaders->OptionalHeader.SizeOfImage;
    Clear();

    if(ImageNtHeaders->OptionalHeader.DataDirectory[IMAGE_DIRECTORY_ENTRY_BASERELOC].VirtualAddress)
        ProcessRelocs(PIMAGE_BASE_RELOCATION(ImageNtHeaders->OptionalHeader.DataDirectory[IMAGE_DIRECTORY_ENTRY_BASERELOC].VirtualAddress + DWORD(ImageBase)));

    if(ImageNtHeaders->OptionalHeader.DataDirectory[IMAGE_DIRECTORY_ENTRY_IMPORT].VirtualAddress)
        ProcessImports(PIMAGE_IMPORT_DESCRIPTOR(ImageNtHeaders->OptionalHeader.DataDirectory[IMAGE_DIRECTORY_ENTRY_IMPORT].VirtualAddress + DWORD(ImageBase)));

    for(int SectionLoop = 0; SectionLoop <= ImageNtHeaders->FileHeader.NumberOfSections - 1; SectionLoop++)
        VirtualProtect(PSections[SectionLoop].VirtualAddress + PCHAR(ImageBase),PSections[SectionLoop].Misc.VirtualSize,GetSectionProtection(PSections[SectionLoop].Characteristics),0);
}


VOID WINAPI CInjectDllEx::DllEntryPoint(LPVOID lpParameter)
{
    TDllLoadInfo LoadInfo = *(TDllLoadInfo*)lpParameter;
    __asm
    {
        xor eax, eax
        push eax
        push DLL_PROCESS_ATTACH
        push LoadInfo.Module
        call LoadInfo.EntryPoint
    }
}

BOOL CInjectDllEx::InjectDllFromMemory(LPVOID Src)
{
#ifndef NDEBUG
    return FALSE;
#endif
    ImageNtHeaders = PIMAGE_NT_HEADERS(DWORD(Src) + DWORD(PIMAGE_DOS_HEADER(Src)->e_lfanew));
    DWORD Offset = 0x10000000;
    LPVOID pModule;
    do
    {
        Offset += 0x10000;
        pModule = VirtualAlloc(LPVOID(ImageNtHeaders->OptionalHeader.ImageBase + Offset),ImageNtHeaders->OptionalHeader.SizeOfImage,MEM_COMMIT|MEM_RESERVE,PAGE_EXECUTE_READWRITE);
        if(pModule)
        {
            VirtualFree(pModule,0,MEM_RELEASE);
            pModule = VirtualAllocEx(this->ProcInfo.hProcess,LPVOID(ImageNtHeaders->OptionalHeader.ImageBase + Offset),ImageNtHeaders->OptionalHeader.SizeOfImage,MEM_COMMIT|MEM_RESERVE,PAGE_EXECUTE_READWRITE);
        }
    }while(!(pModule || Offset > 0x30000000));

    MapLibrary(pModule,Src);
    if(!_ImageBase)
        return FALSE;

    TDllLoadInfo DllLoadInfo;
    DllLoadInfo.Module = _ImageBase;
    DllLoadInfo.EntryPoint = _DllProcAddress;

    WriteProcessMemory(this->ProcInfo.hProcess,pModule,_ImageBase,_ImageSize,0);
    HANDLE hThread = InjectThread(DllEntryPoint,&DllLoadInfo,sizeof(DllLoadInfo));
    if(hThread)
    {
        WaitForSingleObject(hThread,INFINITE);
        CloseHandle(hThread);
        return TRUE;
    }
    return FALSE;
}

BOOL CInjectDllEx::StartAndInject(LPWSTR lpszProcessPath,LPWSTR lpwszStartDir,BOOL bDllInMemory,LPVOID lpDllBuff,LPSTR lpszDllPath,BOOL bReturnResult,DWORD *dwResult)
{
    STARTUPINFOW StartInfo;
    BOOL bReturn = FALSE;

    ZeroMemory(&StartInfo,sizeof(STARTUPINFO));
    StartInfo.cb = sizeof(STARTUPINFO);
    StartInfo.dwFlags = STARTF_USESHOWWINDOW;
    StartInfo.wShowWindow = SW_SHOWNORMAL;
    if(lpwszStartDir[0] == 0 && lpwszStartDir[1] == 0) lpwszStartDir = 0;
	if(CreateProcessW(0,lpszProcessPath,0,0,FALSE,CREATE_SUSPENDED,0,lpwszStartDir,&StartInfo,&ProcInfo))
    {
        if(bDllInMemory)
        {
            bReturn = InjectDllFromMemory(lpDllBuff);
        }
        else
        {
            bReturn = InjectDllFromFile(lpszDllPath);
        }
        
        if(bReturnResult)
            GetExitCodeProcess(this->ProcInfo.hProcess,dwResult);

		if(bReturn == FALSE)
		{
			TerminateProcess(ProcInfo.hProcess,0);
			CloseHandle(ProcInfo.hProcess);
			CloseHandle(ProcInfo.hThread);
			
		}
    }

    GetLastError();
	
	return bReturn;
}
