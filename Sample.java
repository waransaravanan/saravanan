class Sample
{
public static void main(String args[])
{
  System.out.println("Welcome");
  Employee emp;
  emp =  new Employee(100,"Stuart","NewYork");
  emp =  new Employee(104,"David","Melbourne");
  emp =  new Employee(101,"Ruther","Jamaica");
  emp =  new Employee(103,"James","London");
  emp =  new Employee(105,"Kane","Barcelona");
  emp =  new Employee(102,"John","Pretoria");
  

  System.out.println(emp);

  Collections.sort(emp);

  System.out.println(emp);
}
}
