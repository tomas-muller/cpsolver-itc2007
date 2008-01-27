#include <stdio.h>
#include <iostream.h>
#include <math.h>
#include <stdlib.h>
#include <time.h>
#include <fstream.h>
#include <string.h>


main(int argc, char * argv[])
{
int norooms,nostudents, noevents;
int nofeatures;

bool *  * roomfeatures;
bool * * eventfeatures;
int * roomsizes;
int * eventrooms;
int * eventslots;
bool * * attends;       //attends[event][student]
bool * * studentavailability;        //studentavailability[slot][student]
int * eventtypes;
char filename[100];
char timfilename[100];
char slnfilename[100];


if(argc!=2){cout<<"Usage: checksln filename"<<endl;cout<<"Press return to continue"<<endl;
		  char key;
		  cin.get(key);exit(0);}
sscanf(argv[1],"%s",&filename);

int len = strlen(filename);


strcpy(timfilename,filename) ;
timfilename[len]='.';
timfilename[len+1]='t';
timfilename[len+2]='i';
timfilename[len+3]='m';
timfilename[len+4]='\0';

strcpy(slnfilename,filename) ;
slnfilename[len]='.';
slnfilename[len+1]='s';
slnfilename[len+2]='l';
slnfilename[len+3]='n';
slnfilename[len+4]='\0';




ifstream tim;
tim.open(timfilename);
if (!tim ) {
		  cout<< "Couldn't open the file "<<timfilename<<endl;
		  cout<<"Press return to continue"<<endl;
		  char key;
		  cin.get(key);
		  exit(1);
		}

tim>>noevents;
tim>>norooms;
tim>>nofeatures;
tim>>nostudents;

roomsizes = new int[norooms];
eventrooms = new int[noevents];
eventslots = new int[noevents];
eventtypes = new int[noevents];

roomfeatures = new bool*[norooms] ;
for(int i=0;i<norooms;i++) roomfeatures[i] = new bool[nofeatures];

eventfeatures = new bool*[noevents] ;
for(int i=0;i<noevents;i++) eventfeatures[i] = new bool[nofeatures];

attends = new bool *[noevents];
for(int t=0;t<noevents;t++)attends[t] = new bool [nostudents];

studentavailability = new bool *[45];
for(int i=0;i<45;i++) studentavailability[i]=new bool[nostudents];



for(int i=0;i<norooms;i++)tim>>roomsizes[i];


for(int i=0;i<nostudents;i++)for(int j=0;j<noevents;j++)tim>>attends[j][i];

for(int i=0;i<norooms;i++)for(int j=0;j<nofeatures;j++)tim>>roomfeatures[i][j];
for(int i=0;i<noevents;i++)for(int j=0;j<nofeatures;j++)tim>>eventfeatures[i][j];

tim.close();
ifstream sln;
sln.open(slnfilename);


if (!sln) {
		  cout<< "Couldn't open the file "<<slnfilename<<endl;
		  cout<<"Press return to continue"<<endl;
		  char key;
		  cin.get(key);
		  exit(1);
		}
long unplaced=0;
long endofday=0;
for(int i=0;i<noevents;i++)
	{
	sln>>eventslots[i]>>eventrooms[i];
	if ((eventslots[i])== -1)cout<<"Event "<<i<<" does not have a timeslot assigned"<<endl;
	if ((eventrooms[i])== -1)cout<<"Event "<<i<<" does not have a room assigned"<<endl;
	if (((eventrooms[i])== -1)||    ((eventrooms[i])== -1)) unplaced++ ;
   }
sln.close();

long unsuitablerooms=0;
for(int e=0;e<noevents;e++)
	{
   int size =0;
   bool badroom=false;
   for(int g=0;g<nostudents;g++)if(attends[e][g])size++;
   if((eventrooms[e]!=-1)&& (roomsizes[eventrooms[e]]<size))
   	{
      cout<<"Event "<<e<<" requires a room of size "<<size<<endl;
      cout<<"It has been assigned a room ("<<eventrooms[e]<<") of size "<<roomsizes[eventrooms[e]]<<endl;
      badroom=true;
      }
   for(int f=0;f<nofeatures;f++)
   	if((eventrooms[e]!=-1)&&eventfeatures[e][f]&&!roomfeatures[eventrooms[e]][f])
      	{
         cout<<"Event "<<e<<" requires feature "<<f<<endl;
      	cout<<"It has been assigned a room ("<<eventrooms[e]<<") without feature "<<f<<endl;
      	badroom=true;
      	}
   if(badroom)unsuitablerooms++;
   }

long studentclashes=0;
long roomclashes=0;
for(int i=0;i<nostudents;i++)for(int e=0;e<45;e++)studentavailability[e][i]=true;

for(int g = 0; g<nostudents;g++)
   for(int e=0;e<noevents;e++)
   	{
   	for(int f=0;f<e;f++)
   		if((eventslots[e]!=-1)
         	&&(eventslots[f]!=-1)
         	&&attends[e][g]
            &&attends[f][g]
         	&&eventslots[e]==eventslots[f])
         		{
            	cout <<"Student "<<g<<" has to attend both event "<<e<<" and event "<<f<<" in slot "<< eventslots[e]<<endl;
            	studentclashes++;
            	}
      if((eventslots[e]!=-1)&&attends[e][g])studentavailability[eventslots[e]][g]=false;

      }

for(int e=0;e<noevents;e++)
	for(int f=0;f<e;f++)
   	if((eventslots[e]!=-1)
      	&&(eventslots[f]!=-1)
         &&(eventrooms[e]!=-1)
         &&(eventrooms[f]!=-1)
         &&(eventslots[e]==eventslots[f])
         &&(eventrooms[e]==eventrooms[f]))
      		{
         	cout <<"Events "<<e<<" and event "<<f<<" both occur in slot "<< eventslots[e]<<" and room "<<eventrooms[e]<<endl;
         	roomclashes++;
         	}
cout<<endl<<endl<<"Number of unplaced events ="<<unplaced<<endl;
cout <<"Number of unsuitable rooms = "<<unsuitablerooms<<endl;
cout <<"Number of student clashes = "<<studentclashes<<endl;
cout <<"Number of room clashes = "<<roomclashes<<endl<<endl;

long longintensive=0;
for(int g=0;g<nostudents;g++)
	{
  
   for(int d=0;d<5;d++)
   	{
      int count=0;

   	for(int t=0;t<9;t++)
      	{
         int slot=d*9+t;
         if(studentavailability[slot][g]==false)count++;
         else count=0;
         if(count>=3)
         	{
            cout <<"Student "<<g<<" has a set of three events up to slot "<<slot<<endl;
            longintensive++;
            }
         }
      }
   }

 cout<<endl<<endl;
long single=0;
for(int g=0;g<nostudents;g++)
	{
   for(int d=0;d<5;d++)
   	{
      int count=0;
      int badslot=-1;
   	for(int t=0;t<9;t++)
      	{
         int slot=d*9+t;
         if(studentavailability[slot][g]==false)
         	{
            count++;
            badslot=slot;
            }
         }
      if(count==1)
      	{
         cout <<"Student "<<g<<" has an event in slot "<<badslot<<" which is the only one on that day"<<endl;
            single++;
         }

      }
   }
for(int g=0;g<nostudents;g++)
	{
    if(studentavailability[8][g]==false)
   	{
   	cout<<"Student "<<g<<" has an event in slot 8 which is at the end of a day"<<endl;
      endofday++;
      }
    if(studentavailability[17][g]==false)
   	{
   	cout<<"Student "<<g<<" has an event in slot 17 which is at the end of a day"<<endl;
      endofday++;
      }
    if(studentavailability[26][g]==false)
   	{
   	cout<<"Student "<<g<<" has an event in slot 26 which is at the end of a day"<<endl;
      endofday++;
      }
    if(studentavailability[35][g]==false)
   	{
   	cout<<"Student "<<g<<" has an event in slot 35 which is at the end of a day"<<endl;
      endofday++;
      }
    if(studentavailability[44][g]==false)
   	{
   	cout<<"Student "<<g<<" has an event in slot 44 which is at the end of a day"<<endl;
      endofday++;
      }
    }
cout <<endl<<"Penalty for students having three or more events in a row = "<<longintensive<<endl;
cout <<"Penalty for students having single events on a day = "<<single<<endl;
cout <<"Penalty for students having end of day events = "<<endofday<<endl;

cout<<endl<<"TOTAL PENALTY = "<<(longintensive+single+endofday)<<endl<<endl;
if (unplaced+unsuitablerooms+studentclashes+roomclashes)cout<<"This solution file does not give a complete and feasible timetable"<<endl;
else    cout<<"This solution file gives a complete and feasible timetable"<<endl;


delete [] roomsizes;
delete [] eventrooms;
delete [] eventslots;
for(int t=0;t<noevents;t++)delete[]attends[t];
delete [] attends;
for(int i=0;i<45;i++)delete []studentavailability[i];
delete [] studentavailability;
delete []eventtypes;
for(int i=0;i<nofeatures;i++)delete []roomfeatures[i];
delete []roomfeatures;
for(int i=0;i<nofeatures;i++)delete []eventfeatures[i];
delete []eventfeatures;
cout<<"Press return to continue"<<endl;
		  char key;
		  cin.get(key);





}
