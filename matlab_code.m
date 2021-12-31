load C:\Users\iris\Desktop\Εργασία\session2\csv\echo_responses.csv;
load C:\Users\iris\Desktop\Εργασία\session2\csv\arq_responses.csv;
load C:\Users\iris\Desktop\Εργασία\session2\csv\arq_M.csv;
load C:\Users\iris\Desktop\Εργασία\session2\csv\echo_percentages.csv;
load C:\Users\iris\Desktop\Εργασία\session2\csv\arq_percentages.csv;

%G1

figure;
bar(echo_responses);
title('G1 E0451 18-04-2021 15:03');
xlabel('Package');
ylabel('Response time (ms)');
saveas(gcf,'G1.png');

%G2

figure;
bar(arq_responses);
title('G2 Q7799 R2007 18-04-2021 15:09');
xlabel('Package');
ylabel('Response time (ms)');
saveas(gcf,'G2.png');

%G3

figure;
histogram(arq_M);
title('G3 Q7799 R2007 18-04-2021 15:09');
xlabel('Number of NACK required');
ylabel('Total packages');
saveas(gcf,'G3.png');
%}
%Pie-echo

figure;

labels = {'<= 60ms','> 60ms && <= 100ms','> 100ms'};
ax1 = nexttile;
pie(ax1,echo_percentages);
title('Echo Percentages');
lgd = legend(labels);
lgd.Layout.Tile= 'south';
saveas(gcf,'G4.png');


%Pie-arq

figure;
labels = {'<= 75ms','> 75ms && <= 150ms','> 150ms && <=250ms', '>250ms'};
ax1 = nexttile;
pie(ax1,arq_percentages,[0 0 0 1]);
title({'Arq Percentages' '             '});
lgd = legend(labels);
lgd.Layout.Tile= 'south';
saveas(gcf,'G5.png');