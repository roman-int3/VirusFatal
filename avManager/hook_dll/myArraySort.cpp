#include "hook_dll.h"



BOOL myArraySort::Add(word_info *data)
{
	this->myArray::Add((void*)data);
	q_sort(this,1,this->Count());
		return true;
}

void swap(myArray *pArr,int a,int b)
{
	word_info *w = (word_info *)pArr->Get(a);
	void *tmp = pArr->Replace(b,w);
	pArr->Replace(a,tmp);
}

void myArraySort::q_sort(myArray *pArr,int m,int n)
{
	int key,i,j,k,ki;

	if(m < n)
	{
		k = (m+n)/2;
		word_info *w = (word_info *)pArr->Get(k);
		swap(pArr,m,k);
		key = w->x;
		i=m+1;
		j=n;

		while(i <= j)
		{
			w = (word_info *)pArr->Get(i);
			while(i <= n)
			{
				w = (word_info *)pArr->Get(i);
				if(w->x > key) break;
				i++;
			}

			while(j >= m)
			{
				w = (word_info *)pArr->Get(j);
				if(w->x <= key) break;
					j--;
			}

			if(i<j) swap(pArr,i,j);
		}

		swap(pArr,m,j);
		q_sort(pArr,m,j-1);
		q_sort(pArr,j+1,n);
	

	}
}
