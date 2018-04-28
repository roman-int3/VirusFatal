#include "hook_dll.h"
#include "Array.h"

myArray::myArray()
{
	this->pArray = (Array*)GlobalAlloc(GPTR,sizeof(Array));
	this->count = 0;
	this->lastElem = this->pArray;
}

myArray::~myArray()
{
	Array *tmpArray;
	while(this->pArray)
	{
		tmpArray = this->pArray;
		this->pArray = tmpArray->next;
		GlobalFree(tmpArray);
	}
}

void myArray::Empty()
{
	Array *tmpArray;
	while(this->pArray)
	{
		tmpArray = this->pArray;
		this->pArray = tmpArray->next;
		GlobalFree(tmpArray);
	}
	
	this->pArray = (Array*)GlobalAlloc(GPTR,sizeof(Array));
	this->count = 0;
	this->lastElem = this->pArray;
}



BOOL myArray::Add(void *data)
{
	if(this->lastElem == 0 || this->pArray == 0)
	{
		this->pArray = (Array*)GlobalAlloc(GPTR,sizeof(Array));
		this->count = 0;
		this->lastElem = this->pArray;
	}
	
	this->lastElem->data = data;
	this->count++;
	this->lastElem->next = (Array*)GlobalAlloc(GPTR,sizeof(Array));
	this->lastElem = this->lastElem->next;
	return TRUE;

}

int myArray::Count()
{
	return this->count;
}

void* myArray::Get(int index)
{
	index--;
	Array *tmpArray = this->pArray;
	for(int i=0; tmpArray !=0; i++)
	{
		if(i == index)
			return tmpArray->data;
		tmpArray = tmpArray->next;
	}
	return 0;
}

BOOL myArray::Insert(int index,void *data)
{
	index--;
	Array *tmpArray = this->pArray;
	for(int i=0; tmpArray !=0; i++)
	{
		if(i == index)
		{
			void *oldData = tmpArray->data;
			tmpArray->data = data;
			this->Add(oldData);
			return true;
		}
		
		tmpArray = tmpArray->next;
	}
	
	return false;
}

void* myArray::Replace(int index,void *data)
{
	index--;
	Array *tmpArray = this->pArray;
	for(int i=0; tmpArray !=0; i++)
	{
		if(i == index)
		{
			void *oldData = tmpArray->data;
			tmpArray->data = data;
			return oldData;
		}
		
		tmpArray = tmpArray->next;
	}
	
	return false;
}

void* myArray::FindData(void* searchData)
{
	Array *tmpArray = this->pArray;
	for(int i=0; tmpArray !=0; i++)
	{
		if(searchData == tmpArray->data)
			return tmpArray->data;
		tmpArray = tmpArray->next;
	}
	return 0;
}

BOOL myArray::Delete(int index)
{
	Array *prev = 0;
	Array *origin = this->pArray;
	Array *tmpArray = this->pArray;
	index--;
	BOOL result = FALSE;

	for(int i=0; tmpArray !=0; i++)
	{
		if(i == index)
		{
			if(tmpArray->next == 0 && prev != 0)
				prev->next = 0;
			else if(tmpArray->next != 0 && prev != 0)
				prev->next = tmpArray->next;
			
			GlobalFree(tmpArray);
			if(i == 0)
			{
				origin = 0;
			}
			
			this->count--;
			if(index == this->count)
			{
				this->lastElem = prev;
			}
			
			
			result = TRUE;
			break;
		}
		prev = tmpArray;
		tmpArray = tmpArray->next;
	}

	this->pArray = origin;
	return result;
}

BOOL myArray::Delete(void *delElem)
{
	Array *prev = 0;
	Array *origin = this->pArray;
	Array *tmpArray = this->pArray;
	BOOL result = FALSE;

	for(int i=0,index=0; tmpArray !=0; i++,index++)
	{
		if(delElem == tmpArray->data)
		{
			if(tmpArray->next == 0 && prev != 0)
				prev->next = 0;
			else if(tmpArray->next != 0 && prev != 0)
				prev->next = tmpArray->next;
			
			GlobalFree(tmpArray);
			if(i == 0)
			{
				origin = 0;
			}
			
			this->count--;
			if(index == this->count)
			{
				this->lastElem = prev;
			}
			
			
			result = TRUE;
			break;
		}
		prev = tmpArray;
		tmpArray = tmpArray->next;
	}

	this->pArray = origin;
	return result;
}