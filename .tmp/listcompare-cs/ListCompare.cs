using System.Diagnostics;

int N = 200_000;
int RANDOM_GETS = 5_000;
int HEAD_ADDS = 20_000;

var list = new List<int>(N);
var linked = new LinkedList<int>();
for (int i = 0; i < N; i++) { list.Add(i); linked.AddLast(i); }

var rnd = new Random(42);
var idx = new int[RANDOM_GETS];
for (int i = 0; i < RANDOM_GETS; i++) idx[i] = rnd.Next(N);

long sum = 0;

var sw = Stopwatch.StartNew();
foreach (var i in idx) sum += list[i];
long a1 = sw.ElapsedMilliseconds;

// C# LinkedList<T> has no indexer, so walk nodes manually
static int NodeAt(LinkedList<int> l, int index)
{
    var node = l.First!;
    for (int k = 0; k < index; k++) node = node.Next!;
    return node.Value;
}

sw.Restart();
foreach (var i in idx) sum += NodeAt(linked, i);
long l1 = sw.ElapsedMilliseconds;

sw.Restart();
for (int i = 0; i < HEAD_ADDS; i++) list.Insert(0, -1);
long a2 = sw.ElapsedMilliseconds;

sw.Restart();
for (int i = 0; i < HEAD_ADDS; i++) linked.AddFirst(-1);
long l2 = sw.ElapsedMilliseconds;

sw.Restart();
foreach (var v in list) sum += v;
long a3 = sw.ElapsedMilliseconds;

sw.Restart();
foreach (var v in linked) sum += v;
long l3 = sw.ElapsedMilliseconds;

Console.WriteLine($"N = {N:N0}");
Console.WriteLine($"{"operation",-30}{"List<T>",12}{"LinkedList<T>",16}");
Console.WriteLine($"{"random get x " + RANDOM_GETS,-30}{a1 + " ms",12}{l1 + " ms",16}");
Console.WriteLine($"{"head insert x " + HEAD_ADDS,-30}{a2 + " ms",12}{l2 + " ms",16}");
Console.WriteLine($"{"foreach all",-30}{a3 + " ms",12}{l3 + " ms",16}");

// capacity growth of List<T>
var cap = new List<int>();
Console.Write("List<int> capacity growth: ");
int last = cap.Capacity;
for (int i = 0; i < 40; i++)
{
    cap.Add(i);
    if (cap.Capacity != last) { Console.Write(cap.Capacity + " "); last = cap.Capacity; }
}
Console.WriteLine();
if (sum == 42) Console.WriteLine();
