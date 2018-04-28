
#ifndef _MY_ARRAY
#define _MY_ARRAY

typedef struct _Array
{
	void	*data;
	_Array	*next;
}Array;

class myArray
{
public:
	BOOL Add(void *data);
	int Count();
	void *Get(int index);
	BOOL Delete(int index);
	BOOL Delete(void *delElem);
	void Empty();
	myArray();
	~myArray();
	void *FindData(void* searchData);
	BOOL Insert(int index,void *data);
	void* Replace(int index,void *data);
	
protected:
	Array *pArray;
	int count;
	Array *lastElem;

};

#endif